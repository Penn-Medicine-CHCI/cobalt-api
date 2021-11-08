package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.config.IcConfig;
import com.cobaltplatform.ic.backend.model.cobalt.SendCallMessageRequest;
import com.cobaltplatform.ic.backend.model.cobalt.SendSmsMessageRequest;
import com.cobaltplatform.ic.backend.exception.CobaltException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Transmogrify, LLC.
 */
public class CobaltServiceTest {
	@Test
	@Tag("requiresCobalt")
	void sendSmsMessage() throws CobaltException {
		CobaltService cobaltService = CobaltService.getSharedInstance();
		cobaltService.sendSmsMessage(new SendSmsMessageRequest("+12155551212", "just a test"));
	}

	@Test
	@Tag("requiresCobalt")
	void sendCallMessage() throws CobaltException {
		CobaltService cobaltService = new CobaltService("http://localhost:8080", IcConfig.getKeyPair().getPrivate());
		cobaltService.sendCallMessage(new SendCallMessageRequest("+12155551212", "just a test"));
	}
}
