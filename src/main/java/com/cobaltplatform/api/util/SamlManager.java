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

package com.cobaltplatform.api.util;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.security.SamlIdentityProvider;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.model.AttributeConsumingService;
import com.onelogin.saml2.model.RequestedAttribute;
import com.onelogin.saml2.settings.Metadata;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class SamlManager {
	@Nonnull
	private final SamlIdentityProvider samlIdentityProvider;
	@Nonnull
	private final Configuration configuration;

	public SamlManager(@Nonnull SamlIdentityProvider samlIdentityProvider,
										 @Nonnull Configuration configuration) {
		requireNonNull(samlIdentityProvider);
		requireNonNull(configuration);

		this.samlIdentityProvider = samlIdentityProvider;
		this.configuration = configuration;
	}

	@Nonnull
	public void redirectToLogin(@Nonnull HttpServletRequest httpServletRequest,
															@Nonnull HttpServletResponse httpServletResponse,
															@Nullable String returnToUrl) {
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		try {
			Auth auth = new Auth(getSaml2Settings(), httpServletRequest, httpServletResponse);

			if (returnToUrl != null)
				auth.login(returnToUrl);
			else
				auth.login();
		} catch (Exception e) {
			throw new SamlException("Unable to force SAML auth", e);
		}
	}

	@Nonnull
	public void redirectToLogout(@Nonnull HttpServletRequest httpServletRequest,
															 @Nonnull HttpServletResponse httpServletResponse,
															 @Nullable String returnToUrl) {
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		try {
			Auth auth = new Auth(getSaml2Settings(), httpServletRequest, httpServletResponse);

			if (returnToUrl != null)
				auth.logout(returnToUrl);
			else
				auth.logout();
		} catch (Exception e) {
			throw new SamlException("Unable to force SAML logout", e);
		}
	}

	@Nonnull
	public SamlAssertion processAssertion(@Nonnull HttpServletRequest httpServletRequest,
																				@Nonnull HttpServletResponse httpServletResponse) {
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		try {
			Auth auth = new Auth(getSaml2Settings(), httpServletRequest, httpServletResponse);
			auth.processResponse();

			if (auth.isAuthenticated()) {
				SamlAssertion samlAssertion = new SamlAssertion();
				samlAssertion.setNameId(trimToNull(auth.getNameId()));
				samlAssertion.setNameIdFormat(trimToNull(auth.getNameIdFormat()));
				samlAssertion.setSessionIndex(trimToNull(auth.getSessionIndex()));

				DateTime jodaSessionExpiration = auth.getSessionExpiration();
				samlAssertion.setSessionExpiration(jodaSessionExpiration == null ? null : jodaSessionExpiration.toDate().toInstant());

				samlAssertion.setLastMessageId(trimToNull(auth.getLastMessageId()));
				samlAssertion.setLastAssertionId(trimToNull(auth.getLastAssertionId()));

				List<org.joda.time.Instant> jodaLastAssertionNotOnOrAfter = auth.getLastAssertionNotOnOrAfter();
				samlAssertion.setLastAssertionNotOnOrAfter(jodaLastAssertionNotOnOrAfter == null ? Collections.emptyList() : jodaLastAssertionNotOnOrAfter.stream()
						.map(jodaInstant -> jodaInstant.toDate().toInstant())
						.collect(Collectors.toList()));

				String lastResponseXml = trimToNull(auth.getLastResponseXML());

				if (lastResponseXml != null)
					lastResponseXml = XmlUtility.prettyPrintXml(lastResponseXml);

				samlAssertion.setLastResponseXml(lastResponseXml);

				Map<String, List<String>> attributes = auth.getAttributes();

				if (attributes == null)
					attributes = Collections.emptyMap();

				samlAssertion.setAttributes(attributes);

				samlAssertion.setRelayState(trimToNull(httpServletRequest.getParameter("RelayState")));

				return samlAssertion;
			} else {
				List<String> errors = auth.getErrors();

				if (errors.isEmpty())
					throw new SamlException("Unable to process SAML assertion");

				throw new SamlException(format("Unable to process SAML assertion. Errors were: %s", errors));
			}
		} catch (SamlException e) {
			throw e;
		} catch (Exception e) {
			throw new SamlException("Unable to process SAML assertion", e);
		}
	}

	@Nonnull
	public String generateSpMetadata() {
		try {
			Auth auth = new Auth(getSaml2Settings(), null, null);
			Saml2Settings saml2Settings = auth.getSettings();

			String xml = null;

			if (getSamlIdentityProvider().equals(SamlIdentityProvider.COBALT))
				xml = generateDefaultSpMetadata(saml2Settings);
			else
				throw new UnsupportedOperationException(format("Unknown %s value %s", SamlIdentityProvider.class.getSimpleName(), getSamlIdentityProvider().name()));

			List<String> errors = Saml2Settings.validateMetadata(xml);

			if (errors.isEmpty()) {
				xml = XmlUtility.prettyPrintXml(xml);
				return xml;
			}

			throw new SamlException(format("Unable to generate SAML metadata. Errors were: %s", errors));
		} catch (SamlException e) {
			throw e;
		} catch (Exception e) {
			throw new SamlException("Unable to perform SAML metadata generation", e);
		}
	}

	@Nonnull
	protected String generateDefaultSpMetadata(@Nonnull Saml2Settings saml2Settings) throws CertificateEncodingException {
		requireNonNull(saml2Settings);

		String xml = trimToNull(saml2Settings.getSPMetadata());

		if (xml == null)
			throw new SamlException("Generated metadata was blank.");

		return xml;
	}

	@Nonnull
	protected String generateExtendedSpMetadata(@Nonnull Saml2Settings saml2Settings) throws CertificateEncodingException {
		requireNonNull(saml2Settings);

		// To support this: https://wiki.shibboleth.net/confluence/display/CONCEPT/MetadataForSP
		String serviceName = "Cobalt";
		String serviceDescription = "The Cobalt Platform";

		AttributeConsumingService attributeConsumingService = new AttributeConsumingService(serviceName, serviceDescription);
		attributeConsumingService.addRequestedAttribute(new RequestedAttribute("urn:oid:1.3.6.1.4.1.5923.1.1.1.6", "eduPersonPrincipalName", true, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", Collections.emptyList()));
		attributeConsumingService.addRequestedAttribute(new RequestedAttribute("urn:oid:2.5.4.4", "sn", false, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", Collections.emptyList()));
		attributeConsumingService.addRequestedAttribute(new RequestedAttribute("urn:oid:2.5.4.42", "givenName", false, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", Collections.emptyList()));
		attributeConsumingService.addRequestedAttribute(new RequestedAttribute("urn:oid:0.9.2342.19200300.100.1.3", "mail", false, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", Collections.emptyList()));
		attributeConsumingService.addRequestedAttribute(new RequestedAttribute("urn:oid:2.16.840.1.113730.3.1.241", "displayName", false, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", Collections.emptyList()));

		Metadata metadataObj = new Metadata(saml2Settings, null, null, attributeConsumingService);
		String xml = metadataObj.getMetadataString();
		boolean signMetadata = saml2Settings.getSignMetadata();

		if (signMetadata) {
			try {
				xml = Metadata.signMetadata(xml, saml2Settings.getSPkey(), saml2Settings.getSPcert(), saml2Settings.getSignatureAlgorithm(), saml2Settings.getDigestAlgorithm());
			} catch (Exception e) {
				throw new SamlException("Unable to sign metadata.", e);
			}
		}

		if (xml == null)
			throw new SamlException("Generated metadata was blank.");

		return xml;
	}

	@NotThreadSafe
	public static class SamlAssertion {
		@Nullable
		private String nameId;
		@Nullable
		private
		String nameIdFormat;
		@Nullable
		private String sessionIndex;
		@Nullable
		private Instant sessionExpiration;
		@Nullable
		private String relayState;
		@Nullable
		private String lastMessageId;
		@Nullable
		private String lastAssertionId;
		@Nullable
		private List<Instant> lastAssertionNotOnOrAfter;
		@Nullable
		private String lastResponseXml;
		@Nullable
		private Map<String, List<String>> attributes;

		@Nullable
		public String getNameId() {
			return nameId;
		}

		public void setNameId(@Nullable String nameId) {
			this.nameId = nameId;
		}

		@Nullable
		public String getNameIdFormat() {
			return nameIdFormat;
		}

		public void setNameIdFormat(@Nullable String nameIdFormat) {
			this.nameIdFormat = nameIdFormat;
		}

		@Nullable
		public String getSessionIndex() {
			return sessionIndex;
		}

		public void setSessionIndex(@Nullable String sessionIndex) {
			this.sessionIndex = sessionIndex;
		}

		@Nullable
		public Instant getSessionExpiration() {
			return sessionExpiration;
		}

		public void setSessionExpiration(@Nullable Instant sessionExpiration) {
			this.sessionExpiration = sessionExpiration;
		}

		@Nullable
		public String getRelayState() {
			return relayState;
		}

		public void setRelayState(@Nullable String relayState) {
			this.relayState = relayState;
		}

		@Nullable
		public String getLastMessageId() {
			return lastMessageId;
		}

		public void setLastMessageId(@Nullable String lastMessageId) {
			this.lastMessageId = lastMessageId;
		}

		@Nullable
		public String getLastAssertionId() {
			return lastAssertionId;
		}

		public void setLastAssertionId(@Nullable String lastAssertionId) {
			this.lastAssertionId = lastAssertionId;
		}

		@Nullable
		public List<Instant> getLastAssertionNotOnOrAfter() {
			return lastAssertionNotOnOrAfter;
		}

		public void setLastAssertionNotOnOrAfter(@Nullable List<Instant> lastAssertionNotOnOrAfter) {
			this.lastAssertionNotOnOrAfter = lastAssertionNotOnOrAfter;
		}

		@Nullable
		public String getLastResponseXml() {
			return lastResponseXml;
		}

		public void setLastResponseXml(@Nullable String lastResponseXml) {
			this.lastResponseXml = lastResponseXml;
		}

		@Nullable
		public Map<String, List<String>> getAttributes() {
			return attributes;
		}

		public void setAttributes(@Nullable Map<String, List<String>> attributes) {
			this.attributes = attributes;
		}
	}

	@NotThreadSafe
	public static class SamlException extends RuntimeException {
		public SamlException(@Nullable String message) {
			super(message);
		}

		public SamlException(@Nullable Throwable cause) {
			super(cause);
		}

		public SamlException(@Nullable String message,
												 @Nullable Throwable cause) {
			super(message, cause);
		}
	}

	@Nonnull
	protected Saml2Settings getSaml2Settings() {
		return new SettingsBuilder().fromValues(getConfiguration().getSamlSettingsForIdentityProvider(getSamlIdentityProvider()).get()).build();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected SamlIdentityProvider getSamlIdentityProvider() {
		return samlIdentityProvider;
	}
}
