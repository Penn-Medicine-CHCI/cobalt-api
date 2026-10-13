/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalTime;

import static java.util.Objects.requireNonNull;

/**
 * Checks an appointment start against a fixed grid anchored at local midnight.
 * Callers must supply the time in the provider's time zone.
 */
public final class AppointmentStartTimeAlignment {
	private AppointmentStartTimeAlignment() {
	}

	public static boolean isAllowed(@Nonnull LocalTime time, @Nullable Integer intervalInMinutes) {
		requireNonNull(time);

		if (intervalInMinutes == null)
			return true;

		if (intervalInMinutes < 1 || intervalInMinutes > 1440)
			throw new IllegalArgumentException("Start-time interval must be between 1 and 1440 minutes.");

		return time.getSecond() == 0
				&& time.getNano() == 0
				&& (time.getHour() * 60 + time.getMinute()) % intervalInMinutes == 0;
	}
}
