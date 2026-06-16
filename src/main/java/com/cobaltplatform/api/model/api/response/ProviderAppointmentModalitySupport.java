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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class ProviderAppointmentModalitySupport {
	private ProviderAppointmentModalitySupport() {
	}

	@Nonnull
	public static List<ProviderAppointmentModalityId> providerAppointmentModalityIdDisplayOrder() {
		return List.of(ProviderAppointmentModalityId.PHONE, ProviderAppointmentModalityId.VIRTUAL, ProviderAppointmentModalityId.IN_PERSON);
	}

	@Nonnull
	public static Set<ProviderAppointmentModalityId> providerAppointmentModalityIdsFor(@Nonnull Provider provider) {
		requireNonNull(provider);

		EnumSet<ProviderAppointmentModalityId> providerAppointmentModalityIds = EnumSet.noneOf(ProviderAppointmentModalityId.class);
		VideoconferencePlatformId videoconferencePlatformId = provider.getVideoconferencePlatformId();

		if (providerSupportsPhone(provider))
			providerAppointmentModalityIds.add(ProviderAppointmentModalityId.PHONE);

		if (providerSupportsVirtual(provider))
			providerAppointmentModalityIds.add(ProviderAppointmentModalityId.VIRTUAL);

		if (providerAppointmentModalityIds.size() == 0 && videoconferencePlatformId == null)
			providerAppointmentModalityIds.add(ProviderAppointmentModalityId.IN_PERSON);

		return providerAppointmentModalityIds;
	}

	@Nullable
	public static ProviderAppointmentModalityId defaultProviderAppointmentModalityIdFor(@Nonnull Provider provider) {
		requireNonNull(provider);

		Set<ProviderAppointmentModalityId> providerAppointmentModalityIds = providerAppointmentModalityIdsFor(provider);

		if (provider.getVideoconferencePlatformId() == VideoconferencePlatformId.TELEPHONE
				&& providerAppointmentModalityIds.contains(ProviderAppointmentModalityId.PHONE))
			return ProviderAppointmentModalityId.PHONE;

		if (providerSupportsVirtual(provider))
			return ProviderAppointmentModalityId.VIRTUAL;

		if (providerAppointmentModalityIds.size() == 1)
			return providerAppointmentModalityIds.iterator().next();

		return null;
	}

	public static boolean providerSupportsPhone(@Nonnull Provider provider) {
		requireNonNull(provider);

		return provider.getVideoconferencePlatformId() == VideoconferencePlatformId.TELEPHONE
				|| (trimToNull(provider.getPhoneNumber()) != null && !Boolean.TRUE.equals(provider.getDisplayPhoneNumberOnlyForBooking()));
	}

	public static boolean providerSupportsVirtual(@Nonnull Provider provider) {
		requireNonNull(provider);

		return isSupportedVirtualVideoconferencePlatformId(provider.getVideoconferencePlatformId());
	}

	public static boolean isSupportedVirtualVideoconferencePlatformId(@Nullable VideoconferencePlatformId videoconferencePlatformId) {
		return videoconferencePlatformId == VideoconferencePlatformId.EXTERNAL
				|| videoconferencePlatformId == VideoconferencePlatformId.SWITCHBOARD
				|| videoconferencePlatformId == VideoconferencePlatformId.MICROSOFT_TEAMS;
	}
}
