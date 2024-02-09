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

package com.cobaltplatform.api.integration.hl7.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/MSH
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7MessageHeader extends Hl7Object {
	@Nullable
	private String fieldSeparator; // MSH.1 - Field Separator
	@Nullable
	private String encodingCharacters; // MSH.2 - Encoding Characters
	@Nullable
	private Hl7HierarchicDesignator sendingApplication; // MSH.3 - Sending Application
	@Nullable
	private Hl7HierarchicDesignator sendingFacility; // MSH.4 - Sending Facility
	@Nullable
	private Hl7HierarchicDesignator receivingApplication; // MSH.5 - Receiving Application
	@Nullable
	private Hl7HierarchicDesignator receivingFacility; // MSH.6 - Receiving Facility
	@Nullable
	private String dateTimeOfMessage; // MSH.7 - Date / Time of Message
	@Nullable
	private String security; // MSH.8 - Security
	@Nullable
	private Hl7MessageType messageType; // MSH.9 - Message Type
	@Nullable
	private String messageControlId; // MSH.10 - Message Control ID
	@Nullable
	private Hl7ProcessingType processingId; // MSH.11 - Processing ID
	@Nullable
	private Hl7VersionId versionId; // MSH.12 - Version ID
	@Nullable
	private String sequenceNumber; // MSH.13 - Sequence Number
	@Nullable
	private String continuationPointer; // MSH.14 - Continuation Pointer
	@Nullable
	private String acceptAcknowledgementType; // MSH.15 - Accept Acknowledgement Type
	@Nullable
	private String applicationAcknowledgementType; // MSH.16 - Application Acknowledgement Type
	@Nullable
	private String countryCode; // MSH.17 - Country Code
	@Nullable
	private List<String> characterSet; // MSH.18 - Character Set
	@Nullable
	private Hl7CodedElement principalLanguageOfMessage; // MSH.19 - Principal Language of Message
	@Nullable
	private String alternateCharacterSetHandlingScheme; // MSH.20 - Alternate Character Set Handling Scheme
	@Nullable
	private List<Hl7EntityIdentifier> messageProfileIdentifier; // MSH.21 - Message Profile Identifier

	@Nullable
	public String getFieldSeparator() {
		return this.fieldSeparator;
	}

	public void setFieldSeparator(@Nullable String fieldSeparator) {
		this.fieldSeparator = fieldSeparator;
	}

	@Nullable
	public String getEncodingCharacters() {
		return this.encodingCharacters;
	}

	public void setEncodingCharacters(@Nullable String encodingCharacters) {
		this.encodingCharacters = encodingCharacters;
	}

	@Nullable
	public Hl7HierarchicDesignator getSendingApplication() {
		return this.sendingApplication;
	}

	public void setSendingApplication(@Nullable Hl7HierarchicDesignator sendingApplication) {
		this.sendingApplication = sendingApplication;
	}

	@Nullable
	public Hl7HierarchicDesignator getSendingFacility() {
		return this.sendingFacility;
	}

	public void setSendingFacility(@Nullable Hl7HierarchicDesignator sendingFacility) {
		this.sendingFacility = sendingFacility;
	}

	@Nullable
	public Hl7HierarchicDesignator getReceivingApplication() {
		return this.receivingApplication;
	}

	public void setReceivingApplication(@Nullable Hl7HierarchicDesignator receivingApplication) {
		this.receivingApplication = receivingApplication;
	}

	@Nullable
	public Hl7HierarchicDesignator getReceivingFacility() {
		return this.receivingFacility;
	}

	public void setReceivingFacility(@Nullable Hl7HierarchicDesignator receivingFacility) {
		this.receivingFacility = receivingFacility;
	}

	@Nullable
	public String getDateTimeOfMessage() {
		return this.dateTimeOfMessage;
	}

	public void setDateTimeOfMessage(@Nullable String dateTimeOfMessage) {
		this.dateTimeOfMessage = dateTimeOfMessage;
	}

	@Nullable
	public String getSecurity() {
		return this.security;
	}

	public void setSecurity(@Nullable String security) {
		this.security = security;
	}

	@Nullable
	public Hl7MessageType getMessageType() {
		return this.messageType;
	}

	public void setMessageType(@Nullable Hl7MessageType messageType) {
		this.messageType = messageType;
	}

	@Nullable
	public String getMessageControlId() {
		return this.messageControlId;
	}

	public void setMessageControlId(@Nullable String messageControlId) {
		this.messageControlId = messageControlId;
	}

	@Nullable
	public Hl7ProcessingType getProcessingId() {
		return this.processingId;
	}

	public void setProcessingId(@Nullable Hl7ProcessingType processingId) {
		this.processingId = processingId;
	}

	@Nullable
	public Hl7VersionId getVersionId() {
		return this.versionId;
	}

	public void setVersionId(@Nullable Hl7VersionId versionId) {
		this.versionId = versionId;
	}

	@Nullable
	public String getSequenceNumber() {
		return this.sequenceNumber;
	}

	public void setSequenceNumber(@Nullable String sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	@Nullable
	public String getContinuationPointer() {
		return this.continuationPointer;
	}

	public void setContinuationPointer(@Nullable String continuationPointer) {
		this.continuationPointer = continuationPointer;
	}

	@Nullable
	public String getAcceptAcknowledgementType() {
		return this.acceptAcknowledgementType;
	}

	public void setAcceptAcknowledgementType(@Nullable String acceptAcknowledgementType) {
		this.acceptAcknowledgementType = acceptAcknowledgementType;
	}

	@Nullable
	public String getApplicationAcknowledgementType() {
		return this.applicationAcknowledgementType;
	}

	public void setApplicationAcknowledgementType(@Nullable String applicationAcknowledgementType) {
		this.applicationAcknowledgementType = applicationAcknowledgementType;
	}

	@Nullable
	public String getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(@Nullable String countryCode) {
		this.countryCode = countryCode;
	}

	@Nullable
	public List<String> getCharacterSet() {
		return this.characterSet;
	}

	public void setCharacterSet(@Nullable List<String> characterSet) {
		this.characterSet = characterSet;
	}

	@Nullable
	public Hl7CodedElement getPrincipalLanguageOfMessage() {
		return this.principalLanguageOfMessage;
	}

	public void setPrincipalLanguageOfMessage(@Nullable Hl7CodedElement principalLanguageOfMessage) {
		this.principalLanguageOfMessage = principalLanguageOfMessage;
	}

	@Nullable
	public String getAlternateCharacterSetHandlingScheme() {
		return this.alternateCharacterSetHandlingScheme;
	}

	public void setAlternateCharacterSetHandlingScheme(@Nullable String alternateCharacterSetHandlingScheme) {
		this.alternateCharacterSetHandlingScheme = alternateCharacterSetHandlingScheme;
	}

	@Nullable
	public List<Hl7EntityIdentifier> getMessageProfileIdentifier() {
		return this.messageProfileIdentifier;
	}

	public void setMessageProfileIdentifier(@Nullable List<Hl7EntityIdentifier> messageProfileIdentifier) {
		this.messageProfileIdentifier = messageProfileIdentifier;
	}
}