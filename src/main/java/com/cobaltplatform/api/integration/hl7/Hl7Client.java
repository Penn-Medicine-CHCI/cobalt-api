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
import ca.uhn.hl7v2.model.v251.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v251.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v251.message.ORM_O01;
import ca.uhn.hl7v2.model.v251.segment.MSH;
import ca.uhn.hl7v2.model.v251.segment.ORC;
import ca.uhn.hl7v2.parser.Parser;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.CodedElement;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.CommonOrder;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.EntityIdentifier;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.HierarchicDesignator;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.MessageHeader;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.MessageType;
import com.cobaltplatform.api.integration.hl7.model.Hl7OrderMessage.ProcessingType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
		// Messages are in ASCII and generally contain only carriage returns (not CRLF). Here we force newlines
		return new String(bytes, StandardCharsets.US_ASCII).replace("\r", "\r\n").trim();
	}

	@Nonnull
	public Hl7OrderMessage parseOrderMessage(@Nonnull byte[] patientOrderMessage) throws Hl7ParsingException {
		requireNonNull(patientOrderMessage);
		return parseOrderMessage(messageFromBytes(patientOrderMessage));
	}

	@Nonnull
	public Hl7OrderMessage parseOrderMessage(@Nonnull String patientOrderMessage) throws Hl7ParsingException {
		requireNonNull(patientOrderMessage);

		// TODO: determine if these are threadsafe, would be nice to share them across threads
		HapiContext hapiContext = new DefaultHapiContext();
		Parser parser = hapiContext.getGenericParser();

		Message hapiMessage;

		// Patient order messages must have CRLF endings, otherwise parsing will fail.  Ensure that here.
		patientOrderMessage = patientOrderMessage.trim().lines().collect(Collectors.joining("\r\n"));

		try {
			hapiMessage = parser.parse(patientOrderMessage);
		} catch (Exception e) {
			throw new Hl7ParsingException(format("Unable to parse HL7 message:\n%s", patientOrderMessage), e);
		}

		try {
			Hl7OrderMessage orderMessage = new Hl7OrderMessage();

			// See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
			ORM_O01 ormMessage = (ORM_O01) hapiMessage;

			MSH msh = ormMessage.getMSH();

			// See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/MSH
			MessageHeader messageHeader = new MessageHeader();
			messageHeader.setFieldSeparator(trimToNull(msh.getFieldSeparator().getValueOrEmpty()));
			messageHeader.setEncodingCharacters(trimToNull(msh.getEncodingCharacters().getValueOrEmpty()));

			if (HierarchicDesignator.isPresent(msh.getSendingApplication()))
				messageHeader.setSendingApplication(new HierarchicDesignator(msh.getSendingApplication()));

			if (HierarchicDesignator.isPresent(msh.getSendingFacility()))
				messageHeader.setSendingFacility(new HierarchicDesignator(msh.getSendingFacility()));

			if (HierarchicDesignator.isPresent(msh.getReceivingApplication()))
				messageHeader.setReceivingApplication(new HierarchicDesignator(msh.getReceivingApplication()));

			if (HierarchicDesignator.isPresent(msh.getReceivingFacility()))
				messageHeader.setReceivingFacility(new HierarchicDesignator(msh.getReceivingFacility()));

			messageHeader.setDateTimeOfMessage(trimToNull(msh.getDateTimeOfMessage().getTime().getValue()));
			messageHeader.setSecurity(trimToNull(msh.getSecurity().getValueOrEmpty()));

			if (MessageType.isPresent(msh.getMessageType()))
				messageHeader.setMessageType(new MessageType(msh.getMessageType()));

			messageHeader.setMessageControlId(trimToNull(msh.getMessageControlID().getValueOrEmpty()));

			if (ProcessingType.isPresent(msh.getProcessingID()))
				messageHeader.setProcessingId(new ProcessingType(msh.getProcessingID()));

			if (Hl7OrderMessage.VersionId.isPresent(msh.getVersionID()))
				messageHeader.setVersionId(new Hl7OrderMessage.VersionId(msh.getVersionID()));

			messageHeader.setSequenceNumber(trimToNull(msh.getSequenceNumber().getValue()));
			messageHeader.setContinuationPointer(trimToNull(msh.getContinuationPointer().getValue()));
			messageHeader.setAcceptAcknowledgementType(trimToNull(msh.getAcceptAcknowledgmentType().getValueOrEmpty()));
			messageHeader.setApplicationAcknowledgementType(trimToNull(msh.getApplicationAcknowledgmentType().getValueOrEmpty()));
			messageHeader.setCountryCode(trimToNull(msh.getCountryCode().getValueOrEmpty()));

			if (msh.getCharacterSet() != null && msh.getCharacterSet().length > 0)
				messageHeader.setCharacterSet(Arrays.stream(msh.getCharacterSet())
						.map(cs -> trimToNull(cs.getValueOrEmpty()))
						.filter(cs -> cs != null)
						.collect(Collectors.toList()));


			if (CodedElement.isPresent(msh.getPrincipalLanguageOfMessage()))
				messageHeader.setPrincipalLanguageOfMessage(new CodedElement(msh.getPrincipalLanguageOfMessage()));

			messageHeader.setAlternateCharacterSetHandlingScheme(trimToNull(msh.getAlternateCharacterSetHandlingScheme().getValueOrEmpty()));

			if (msh.getMessageProfileIdentifier() != null && msh.getMessageProfileIdentifier().length > 0)
				messageHeader.setMessageProfileIdentifier(Arrays.stream(msh.getMessageProfileIdentifier())
						.map(mpi -> EntityIdentifier.isPresent(mpi) ? new EntityIdentifier(mpi) : null)
						.filter(mpi -> mpi != null)
						.collect(Collectors.toList()));

			orderMessage.setMessageHeaderSegment(messageHeader);

			ORM_O01_ORDER order = ormMessage.getORDER();
			ORC orc = order.getORC();

			// See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/ORC
			CommonOrder commonOrder = new CommonOrder();
			commonOrder.setOrderControl(trimToNull(orc.getOrc1_OrderControl().getValueOrEmpty()));
			commonOrder.setPlacerOrderNumber(trimToNull(orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValue()));
			commonOrder.setFillerOrderNumber(trimToNull(orc.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().getValue()));
			commonOrder.setPlacerGroupNumber(trimToNull(orc.getOrc4_PlacerGroupNumber().getEi1_EntityIdentifier().getValue()));
			commonOrder.setOrderStatus(trimToNull(orc.getOrc5_OrderStatus().getValue()));

			orderMessage.setCommonOrderSegment(commonOrder);

			ORM_O01_PATIENT patient = ormMessage.getPATIENT();

			// String patientId = patient.getPID().getPatientID().getIDNumber().getValue();
			// String patientIdType = patient.getPID().getPatientID().getIdentifierTypeCode().getValue();

			return orderMessage;
		} catch (Exception e) {
			throw new Hl7ParsingException(format("Encountered an unexpected problem while processing HL7 message:\n%s", patientOrderMessage), e);
		}
	}
}
