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

import com.cobaltplatform.api.integration.hl7.model.event.Hl7GeneralOrderTriggerEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class Hl7ClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testHl7Parsing() throws Exception {
		byte[] generalOrderHl7 = Files.readAllBytes(Path.of("localstack/secrets/patient-order-hl7/To_COBALT_Ord_20240208_09.txt"));

		Hl7Client hl7Client = new Hl7Client();
		String generalOrderHl7AsString = hl7Client.messageFromBytes(generalOrderHl7);
		Hl7GeneralOrderTriggerEvent generalOrder = hl7Client.parseGeneralOrder(generalOrderHl7AsString);

		System.out.println(generalOrderHl7AsString);
		System.out.println(generalOrder.getOrders().get(0).getOrderDetail().getOrderDetailSegment().getObservationRequest());
	}
}
