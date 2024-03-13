/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.integration.hl7;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.message.ORM_O01;
import ca.uhn.hl7v2.parser.Parser;
import com.cobaltplatform.api.integration.hl7.model.event.Hl7GeneralOrderTriggerEvent;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.soklet.util.StringUtils.trimToNull;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * See https://github.com/hapifhir/hapi-hl7v2 for details.
 *
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class Hl7Client {
	@Nonnull
	public String messageFromBytes(@Nonnull byte[] bytes) {
		requireNonNull(bytes);
		// Messages should be in ASCII (but in practice, Windows-1252) and generally contain only carriage returns (not CRLF). Here we force newlines.
		return new String(bytes, Charset.forName("Windows-1252")).replace("\r", "\r\n").trim();
	}

	@Nonnull
	public Hl7GeneralOrderTriggerEvent parseGeneralOrder(@Nonnull byte[] generalOrderHl7) throws Hl7ParsingException {
		requireNonNull(generalOrderHl7);
		return parseGeneralOrder(messageFromBytes(generalOrderHl7));
	}

	@Nonnull
	public Hl7GeneralOrderTriggerEvent parseGeneralOrder(@Nonnull String generalOrderHl7AsString) throws Hl7ParsingException {
		requireNonNull(generalOrderHl7AsString);

		// TODO: determine if HapiContext/Parser instances are threadsafe, would be nice to share them across threads
		try (HapiContext hapiContext = new DefaultHapiContext()) {
			Parser parser = hapiContext.getGenericParser();
			Message message;

			// Patient order messages must have CRLF endings, otherwise parsing will fail.  Ensure that here.
			generalOrderHl7AsString = generalOrderHl7AsString.trim().lines().collect(Collectors.joining("\r\n"));

			// Rewrite illegally-formatted messages where GT1 (guarantor) appears before IN1, IN2, IN3 (insurance)
			// by swapping them
			generalOrderHl7AsString = ensureInsuranceAppearsBeforeGuarantorForGeneralOrderHl7(generalOrderHl7AsString);

			try {
				message = parser.parse(generalOrderHl7AsString);
			} catch (Exception e) {
				throw new Hl7ParsingException(format("Unable to parse HL7 message:\n%s", generalOrderHl7AsString), e);
			}

			try {
				final String SUPPORTED_HL7_VERSION = "2.5.1";
				String messageVersion = trimToNull(message.getVersion());

				// See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
				if (!SUPPORTED_HL7_VERSION.equals(messageVersion)) {
					throw new Hl7ParsingException(format("Supported HL7 version is %s but received message with version %s",
							SUPPORTED_HL7_VERSION, messageVersion == null ? "[unknown]" : messageVersion));
				}

				ORM_O01 ormMessage = (ORM_O01) message;

				if (!Hl7GeneralOrderTriggerEvent.isPresent(ormMessage))
					throw new Hl7ParsingException(format("No %s message data was found", ORM_O01.class.getSimpleName()));

				return new Hl7GeneralOrderTriggerEvent(ormMessage);
			} catch (Exception e) {
				throw new Hl7ParsingException(format("Encountered an unexpected problem while processing HL7 message:\n%s", generalOrderHl7AsString), e);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	public String ensureInsuranceAppearsBeforeGuarantorForGeneralOrderHl7(@Nonnull String generalOrderHl7AsString) {
		requireNonNull(generalOrderHl7AsString);

		List<String> originalLines = new ArrayList<>(Arrays.asList(generalOrderHl7AsString.split("\\r\\n")));
		List<String> rewrittenLines = new ArrayList<>(originalLines.size());
		String guarantorLineToReposition = null;
		boolean alreadyEncounteredInsuranceLine = false;
		boolean previousLineWasInsuranceLine = false;

		for (String originalLine : originalLines) {
			if (originalLine.startsWith("GT1|")) {
				if (alreadyEncounteredInsuranceLine) {
					// The guarantor line is already in the right place, because insurance info came first.
					// Don't need to do anything special.
					rewrittenLines.add(originalLine);
				} else {
					// We haven't seen an insurance line yet - hold on to the guarantor line so we can
					// later insert it after the last insurance line
					guarantorLineToReposition = originalLine;
				}
			} else if (originalLine.startsWith("IN1|") || originalLine.startsWith("IN2|") || originalLine.startsWith("IN3|")) {
				// Insurance line?  We can have many of them.  Keep track of state so when to re-insert the guarantor line (if needed)
				alreadyEncounteredInsuranceLine = true;
				previousLineWasInsuranceLine = true;
				rewrittenLines.add(originalLine);
			} else {
				// If we are not in a guarantor or insurance line, and we just finished the insurance section and have a guarantor
				// to insert, do it.
				if (guarantorLineToReposition != null && previousLineWasInsuranceLine) {
					rewrittenLines.add(guarantorLineToReposition);
					guarantorLineToReposition = null;
				}

				// Write the original line
				rewrittenLines.add(originalLine);
			}
		}

		return rewrittenLines.stream().collect(Collectors.joining("\r\n"));
	}
}
