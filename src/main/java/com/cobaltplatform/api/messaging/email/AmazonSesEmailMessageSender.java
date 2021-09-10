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

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
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
	private final AmazonSimpleEmailService amazonSimpleEmailService;
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
	public void sendMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);

		Map<String, Object> messageContext = new HashMap<>(emailMessage.getMessageContext());
		// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/local/emails/button-start-appointment@2x.jpg
		messageContext.put("staticFileUrlPrefix", format(" https://%s.s3.%s.amazonaws.com/%s/emails",
				getConfiguration().getAmazonS3BucketName(), getConfiguration().getAmazonS3Region().getName(), getConfiguration().getEnvironment()));
		messageContext.put("copyrightYear", LocalDateTime.now(ZoneId.of("America/New_York")).getYear());

		String fromAddress = emailMessage.getFromAddress().isPresent() ? emailMessage.getFromAddress().get() : getDefaultFromAddress();
		String subject = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "subject", emailMessage.getLocale(), messageContext).get();
		String body = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "body", emailMessage.getLocale(), messageContext).get();

		List<String> logMessages = new ArrayList<>(7);
		logMessages.add(format("Sending '%s' email using Amazon SES...", emailMessage.getMessageTemplate()));
		logMessages.add(format("From: %s", fromAddress));

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
			mimeMessage.addFrom(new Address[] { normalizedFromAddress });
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
			RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

			SendRawEmailRequest request = new SendRawEmailRequest()
					.withRawMessage(rawMessage);

			SendRawEmailResult result = getAmazonSimpleEmailService().sendRawEmail(request);

			getLogger().info("Successfully sent email (message ID {}) in {} ms.", result.getMessageId(), System.currentTimeMillis() - time);
		} catch (IOException | MessagingException e) {
			throw new RuntimeException(format("Unable to send %s", emailMessage), e);
		}
	}

	@Nonnull
	protected Address[] toAddresses(@Nonnull List<String> emailAddresses) {
		requireNonNull(emailAddresses);

		Address[] addresses = new Address[emailAddresses.size()];

		for (int i = 0; i < emailAddresses.size(); ++i) {
			String emailAddress = emailAddresses.get(i);
			try {
				if(emailAddress != null)
					addresses[i] = new InternetAddress(emailAddress);
			} catch (AddressException e) {
				throw new RuntimeException(format("Unable to parse email address '%s'", emailAddress), e);
			}
		}

		return addresses;
	}

	@Nonnull
	protected AmazonSimpleEmailService createAmazonSimpleEmailService() {
		AmazonSimpleEmailServiceClientBuilder builder = AmazonSimpleEmailServiceClientBuilder.standard();

		if (getConfiguration().getAmazonUseLocalstack()) {
			builder.withEndpointConfiguration(new EndpointConfiguration(format("http://localhost:%d", /* getConfiguration().getAmazonLocalstackPort() */ 4579), getConfiguration().getAmazonSesRegion().getName()))
					.withCredentials(getConfiguration().getAmazonCredentialsProvider());
		} else {
			builder.withCredentials(getConfiguration().getAmazonCredentialsProvider())
					.withRegion(getConfiguration().getAmazonSesRegion().getName());
		}

		return builder.build();
	}

	@Nonnull
	protected AmazonSimpleEmailService getAmazonSimpleEmailService() {
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