/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ProviderAppointmentModalitySupportTests {
	@Test
	public void virtualOnlyProviderDoesNotOfferPhoneWhenContactNumberIsPresent() {
		Provider provider = new Provider();
		provider.setVideoconferencePlatformId(VideoconferencePlatformId.SWITCHBOARD);
		provider.setPhoneNumber("+12155550100");
		provider.setDisplayPhoneNumberOnlyForBooking(false);
		provider.setVirtualAppointmentsOnly(true);

		assertEquals(Set.of(ProviderAppointmentModalityId.VIRTUAL),
				ProviderAppointmentModalitySupport.providerAppointmentModalityIdsFor(provider));
		assertEquals(ProviderAppointmentModalityId.VIRTUAL,
				ProviderAppointmentModalitySupport.defaultProviderAppointmentModalityIdFor(provider));
		assertFalse(ProviderAppointmentModalitySupport.providerSupportsPhone(provider));
	}

	@Test
	public void virtualOnlyProviderDoesNotFallBackToInPersonWithoutVideoPlatform() {
		Provider provider = new Provider();
		provider.setVirtualAppointmentsOnly(true);

		assertEquals(Set.of(), ProviderAppointmentModalitySupport.providerAppointmentModalityIdsFor(provider));
	}
}
