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
import com.cobaltplatform.api.util.JsonMapper;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ConsoleEmailMessageSender implements MessageSender<EmailMessage> {
	@Nonnull
	private final HandlebarsTemplater handlebarsTemplater;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Logger logger;

	public ConsoleEmailMessageSender(@Nonnull HandlebarsTemplater handlebarsTemplater,
																	 @Nonnull Configuration configuration) {
		requireNonNull(handlebarsTemplater);
		requireNonNull(configuration);

		this.handlebarsTemplater = handlebarsTemplater;
		this.configuration = configuration;
		this.jsonMapper = new JsonMapper();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public String sendMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);

		Map<String, Object> messageContext = emailMessage.getMessageContext();
		String fromAddress = emailMessage.getFromAddress().isPresent() ? emailMessage.getFromAddress().get() : getConfiguration().getEmailDefaultFromAddress();
		String replyToAddress = emailMessage.getReplyToAddress().orElse(null);
		String subject = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "subject", emailMessage.getLocale(), messageContext).get();
		String body = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "body", emailMessage.getLocale(), messageContext).get();

		List<String> logMessages = new ArrayList<>(8);
		logMessages.add(format("Fake-sending '%s' email...", emailMessage.getMessageTemplate()));
		logMessages.add(format("From: %s", fromAddress));

		if (replyToAddress != null)
			logMessages.add(format("Reply-To: %s", replyToAddress));

		if (emailMessage.getToAddresses().size() > 0)
			logMessages.add(format("To: %s", emailMessage.getToAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getCcAddresses().size() > 0)
			logMessages.add(format("CC: %s", emailMessage.getCcAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getBccAddresses().size() > 0)
			logMessages.add(format("BCC: %s", emailMessage.getBccAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getMessageContext() != null && emailMessage.getMessageContext().size() > 0)
			logMessages.add(format("Context: %s", getJsonMapper().toJson(emailMessage.getMessageContext())));

		logMessages.add(format("Subject: %s", subject.trim()));
		logMessages.add(format("Body:\n%s", body.trim()));

		getLogger().info(logMessages.stream().collect(Collectors.joining("\n")));

		final String FAKE_SMTP_HOST = "localhost";
		final int FAKE_SMTP_PORT = 1025;

		if (!isTcpPortAvailable(FAKE_SMTP_HOST, FAKE_SMTP_PORT)) {
			getLogger().debug("Sending as SMTP over {}:{}...", FAKE_SMTP_HOST, FAKE_SMTP_PORT);

			Properties properties = System.getProperties();
			properties.put("mail.smtp.host", FAKE_SMTP_HOST);
			properties.put("mail.smtp.port", String.valueOf(FAKE_SMTP_PORT));

			try {
				Address normalizedFromAddress = fromAddress.equals(getConfiguration().getEmailDefaultFromAddress()) ? new InternetAddress(fromAddress, "Cobalt") : new InternetAddress(fromAddress);

				Session session = Session.getDefaultInstance(properties);
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

				Transport.send(mimeMessage);

				getLogger().info("Sent SMTP message on {}:{}", FAKE_SMTP_HOST, FAKE_SMTP_PORT);
			} catch (Exception e) {
				getLogger().warn(format("Unable to send email on %s:%d", FAKE_SMTP_HOST, FAKE_SMTP_PORT), e);
			}
		}

		return UUID.randomUUID().toString();
	}

	@Nonnull
	protected Boolean isTcpPortAvailable(@Nonnull String host,
																			 @Nonnull Integer port) {
		requireNonNull(host);
		requireNonNull(port);

		try (ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.setReuseAddress(false);
			serverSocket.bind(new InetSocketAddress(InetAddress.getByName(host), port), 1);
			return true;
		} catch (Exception e) {
			return false;
		}
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
	@Override
	public MessageVendorId getMessageVendorId() {
		return MessageVendorId.UNSPECIFIED;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.EMAIL;
	}

	@Nonnull
	protected HandlebarsTemplater getHandlebarsTemplater() {
		return this.handlebarsTemplater;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}