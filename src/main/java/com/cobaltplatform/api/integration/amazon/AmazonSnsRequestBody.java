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

package com.cobaltplatform.api.integration.amazon;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AmazonSnsRequestBody {
	// {
	//  "Type" : "SubscriptionConfirmation",
	//  "MessageId" : "xxx",
	//  "Token" : "xxx",
	//  "TopicArn" : "arn:aws:sns:us-east-1:xxx",
	//  "Message" : "You have chosen to subscribe to the topic arn:aws:sns:us-east-1:xxx.\nTo confirm the subscription, visit the SubscribeURL included in this message.",
	//  "SubscribeURL" : "https://sns.us-east-1.amazonaws.com/?Action=ConfirmSubscription&TopicArn=xxx",
	//  "Timestamp" : "2023-05-12T16:18:27.837Z",
	//  "SignatureVersion" : "1",
	//  "Signature" : "xxx",
	//  "SigningCertURL" : "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-xxx.pem"
	// }

	@Nullable
	@SerializedName("Type")
	private String type;
	@Nullable
	@SerializedName("MessageId")
	private String messageId;
	@Nullable
	@SerializedName("Token")
	private String token;
	@Nullable
	@SerializedName("TopicArn")
	private String topicArn;
	@Nullable
	@SerializedName("Message")
	private String message;
	@Nullable
	@SerializedName("SubscribeURL")
	private String subscribeUrl;
	@Nullable
	@SerializedName("Timestamp")
	private String timestamp;
	@Nullable
	@SerializedName("SignatureVersion")
	private String signatureVersion;
	@Nullable
	@SerializedName("Signature")
	private String signature;
	@Nullable
	@SerializedName("SigningCertURL")
	private String signingCertUrl;

	@Nullable
	public String getType() {
		return this.type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public String getMessageId() {
		return this.messageId;
	}

	public void setMessageId(@Nullable String messageId) {
		this.messageId = messageId;
	}

	@Nullable
	public String getToken() {
		return this.token;
	}

	public void setToken(@Nullable String token) {
		this.token = token;
	}

	@Nullable
	public String getTopicArn() {
		return this.topicArn;
	}

	public void setTopicArn(@Nullable String topicArn) {
		this.topicArn = topicArn;
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Nullable
	public String getSubscribeUrl() {
		return this.subscribeUrl;
	}

	public void setSubscribeUrl(@Nullable String subscribeUrl) {
		this.subscribeUrl = subscribeUrl;
	}

	@Nullable
	public String getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(@Nullable String timestamp) {
		this.timestamp = timestamp;
	}

	@Nullable
	public String getSignatureVersion() {
		return this.signatureVersion;
	}

	public void setSignatureVersion(@Nullable String signatureVersion) {
		this.signatureVersion = signatureVersion;
	}

	@Nullable
	public String getSignature() {
		return this.signature;
	}

	public void setSignature(@Nullable String signature) {
		this.signature = signature;
	}

	@Nullable
	public String getSigningCertUrl() {
		return this.signingCertUrl;
	}

	public void setSigningCertUrl(@Nullable String signingCertUrl) {
		this.signingCertUrl = signingCertUrl;
	}
}
