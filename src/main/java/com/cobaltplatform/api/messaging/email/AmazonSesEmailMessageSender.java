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

package com.cobaltplatform.api.messaging.email;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AmazonSesEmailMessageSender implements MessageSender<EmailMessage> {
	@Nonnull
	private final HandlebarsTemplater handlebarsTemplater;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final SesClient amazonSimpleEmailService;
	@Nonnull
	private final String defaultFromAddress;
	@Nonnull
	private final Logger logger;

	public AmazonSesEmailMessageSender(@Nonnull HandlebarsTemplater handlebarsTemplater,
																		 @Nonnull Configuration configuration) {
		requireNonNull(handlebarsTemplater);
		requireNonNull(configuration);

		this.handlebarsTemplater = handlebarsTemplater;
		this.configuration = configuration;
		this.defaultFromAddress = configuration.getEmailDefaultFromAddress();
		this.amazonSimpleEmailService = createAmazonSimpleEmailService();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public String sendMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);

		Map<String, Object> messageContext = emailMessage.getMessageContext();
		String fromAddress = emailMessage.getFromAddress().isPresent() ? emailMessage.getFromAddress().get() : getDefaultFromAddress();
		String replyToAddress = emailMessage.getReplyToAddress().orElse(null);
		String subject = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "subject", emailMessage.getLocale(), messageContext).get();
		String body = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "body", emailMessage.getLocale(), messageContext).get();

		List<String> logMessages = new ArrayList<>(7);
		logMessages.add(format("Sending '%s' email using Amazon SES...", emailMessage.getMessageTemplate()));
		logMessages.add(format("From: %s", fromAddress));

		if (replyToAddress != null)
			logMessages.add(format("Reply-To: %s", replyToAddress));

		if (emailMessage.getToAddresses().size() > 0)
			logMessages.add(format("To: %s", emailMessage.getToAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getCcAddresses().size() > 0)
			logMessages.add(format("CC: %s", emailMessage.getCcAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getBccAddresses().size() > 0)
			logMessages.add(format("BCC: %s", emailMessage.getBccAddresses().stream().collect(Collectors.joining(", "))));

		logMessages.add(format("Subject: %s", subject));
		logMessages.add(format("Body:\n%s", body));

		getLogger().info(logMessages.stream().collect(Collectors.joining("\n")));

		long time = System.currentTimeMillis();

		try {
			Address normalizedFromAddress = fromAddress.equals(getDefaultFromAddress()) ? new InternetAddress(fromAddress, "Cobalt") : new InternetAddress(fromAddress);

			Session session = Session.getDefaultInstance(new Properties());
			MimeMessage mimeMessage = new MimeMessage(session);
			mimeMessage.addFrom(new Address[]{normalizedFromAddress});

			if (replyToAddress != null)
				mimeMessage.setReplyTo(new Address[]{new InternetAddress(replyToAddress)});

			mimeMessage.addRecipients(Message.RecipientType.TO, toAddresses(emailMessage.getToAddresses()));
			mimeMessage.addRecipients(Message.RecipientType.CC, toAddresses(emailMessage.getCcAddresses()));
			mimeMessage.addRecipients(Message.RecipientType.BCC, toAddresses(emailMessage.getBccAddresses()));
			mimeMessage.setSubject(subject, "UTF-8");

			MimeMultipart mimeMultipart = new MimeMultipart();
			BodyPart bodyPart = new MimeBodyPart();
			bodyPart.setContent(body, "text/html; charset=UTF-8");
			mimeMultipart.addBodyPart(bodyPart);

			for (EmailAttachment emailAttachment : emailMessage.getEmailAttachments()) {
				MimeBodyPart attachmentPart = new MimeBodyPart();
				attachmentPart.setFileName(emailAttachment.getFilename());
				DataSource dataSource = new ByteArrayDataSource(emailAttachment.getData(), emailAttachment.getContentType());
				attachmentPart.setDataHandler(new DataHandler(dataSource));
				mimeMultipart.addBodyPart(attachmentPart);
			}

			mimeMessage.setContent(mimeMultipart);

			if (mimeMessage.getSize() > 10_000_000)
				throw new RuntimeException("Email is too large, must be smaller than 10MB");

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			mimeMessage.writeTo(outputStream);
			RawMessage rawMessage = RawMessage.builder()
					.data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray())))
					.build();

			SendRawEmailRequest request = SendRawEmailRequest.builder()
					.source(fromAddress)
					.rawMessage(rawMessage)
					// We specify a configuration set, which has SNS topic[s] attached to it, which allows
					// AWS to send us webhook notifications on successful deliveries, bounces, etc.
					// Environment-specific configuration sets are needed because the same SES Verified Identity might be used
					// across environments and we need a way to route SNS webhooks accordingly
					// See https://docs.aws.amazon.com/ses/latest/dg/using-configuration-sets-in-email.html
					.configurationSetName(getConfiguration().getAmazonSesConfigurationSetName().orElse(null))
					.build();

			SendRawEmailResponse result = getAmazonSimpleEmailService().sendRawEmail(request);

			getLogger().info("Successfully sent email (message ID {}) in {} ms.", result.messageId(), System.currentTimeMillis() - time);

			return result.messageId();
		} catch (IOException | MessagingException e) {
			throw new RuntimeException(format("Unable to send %s", emailMessage), e);
		}
	}

	@Nonnull
	@Override
	public MessageVendorId getMessageVendorId() {
		return MessageVendorId.AMAZON_SES;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.EMAIL;
	}

	@Nonnull
	protected Address[] toAddresses(@Nonnull List<String> emailAddresses) {
		requireNonNull(emailAddresses);

		Address[] addresses = new Address[emailAddresses.size()];

		for (int i = 0; i < emailAddresses.size(); ++i) {
			String emailAddress = emailAddresses.get(i);
			try {
				if (emailAddress != null)
					addresses[i] = new InternetAddress(emailAddress);
			} catch (AddressException e) {
				throw new RuntimeException(format("Unable to parse email address '%s'", emailAddress), e);
			}
		}

		return addresses;
	}

	@Nonnull
	protected SesClient createAmazonSimpleEmailService() {
		SesClientBuilder builder = SesClient.builder()
				.region(getConfiguration().getAmazonSesRegion());

		if (getConfiguration().getAmazonUseLocalstack()) {
			builder.endpointOverride(URI.create(format("http://localhost:%d", getConfiguration().getAmazonLocalstackPort())));
		}

		return builder.build();
	}


	@Nonnull
	protected SesClient getAmazonSimpleEmailService() {
		return amazonSimpleEmailService;
	}

	@Nonnull
	protected HandlebarsTemplater getHandlebarsTemplater() {
		return handlebarsTemplater;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected String getDefaultFromAddress() {
		return defaultFromAddress;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}