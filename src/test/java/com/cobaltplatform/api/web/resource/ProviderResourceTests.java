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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.util.ValidationException;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderResourceTests {
	@Test
	public void providerAvailabilityDateRangeDefaultsToTodayThroughNinetyDays() {
		ZoneId timeZone = ZoneId.of("America/New_York");
		LocalDate dateBeforeResolution = LocalDate.now(timeZone);

		ProviderAvailabilityResource.AvailabilityDateRange dateRange =
				ProviderAvailabilityResource.availabilityDateRangeFor(Optional.empty(), Optional.empty(), timeZone);

		LocalDate dateAfterResolution = LocalDate.now(timeZone);

		assertTrue(dateRange.getStartDate().equals(dateBeforeResolution)
				|| dateRange.getStartDate().equals(dateAfterResolution));
		assertEquals(dateRange.getStartDate().plusDays(90), dateRange.getEndDate());
	}

	@Test
	public void providerAvailabilityDateRangeDefaultsEndDateFromSuppliedStartDate() {
		LocalDate startDate = LocalDate.of(2026, 1, 1);

		ProviderAvailabilityResource.AvailabilityDateRange dateRange =
				ProviderAvailabilityResource.availabilityDateRangeFor(Optional.of(startDate), Optional.empty(), ZoneId.of("America/New_York"));

		assertEquals(startDate, dateRange.getStartDate());
		assertEquals(startDate.plusDays(90), dateRange.getEndDate());
	}

	@Test(expected = ValidationException.class)
	public void providerAvailabilityDateRangeRejectsInvalidRange() {
		ProviderAvailabilityResource.availabilityDateRangeFor(Optional.of(LocalDate.of(2026, 1, 2)),
				Optional.of(LocalDate.of(2026, 1, 1)), ZoneId.of("America/New_York"));
	}

	@Test
	public void providerSupportRolesMatchFeature() {
		assertTrue(ProviderAvailabilityResource.providerSupportRolesMatchFeature(List.of(SupportRoleId.CLINICIAN),
				List.of(SupportRoleId.COACH, SupportRoleId.CLINICIAN)));
	}

	@Test
	public void providerSupportRolesMatchFeatureRejectsNoOverlap() {
		assertFalse(ProviderAvailabilityResource.providerSupportRolesMatchFeature(List.of(SupportRoleId.CLINICIAN),
				List.of(SupportRoleId.COACH)));
	}

	@Test
	public void providerSupportRolesMatchFeatureRejectsFeatureWithoutSupportRoles() {
		assertFalse(ProviderAvailabilityResource.providerSupportRolesMatchFeature(List.of(),
				List.of(SupportRoleId.CLINICIAN)));
	}

	@Test
	public void providerSearchArgumentsAbsent() {
		assertTrue(ProviderResource.providerSearchArgumentsAbsent(Optional.empty(), null));
		assertFalse(ProviderResource.providerSearchArgumentsAbsent(Optional.of(FeatureId.THERAPY), null));
		assertFalse(ProviderResource.providerSearchArgumentsAbsent(Optional.empty(), "na"));
	}

	@Test
	public void normalizeInstitutionLocationIdForProviderSearch() {
		assertNull(ProviderResource.normalizeInstitutionLocationIdForProviderSearch(Optional.empty()));
		assertNull(ProviderResource.normalizeInstitutionLocationIdForProviderSearch(Optional.of(" ")));
		assertEquals("na", ProviderResource.normalizeInstitutionLocationIdForProviderSearch(Optional.of(" na ")));
	}

	@Test
	public void parseInstitutionLocationIdForProviderSearch() {
		UUID institutionLocationId = UUID.randomUUID();

		assertEquals(institutionLocationId, ProviderResource.parseInstitutionLocationIdForProviderSearch(institutionLocationId.toString()));
		assertNull(ProviderResource.parseInstitutionLocationIdForProviderSearch("na"));
		assertNull(ProviderResource.parseInstitutionLocationIdForProviderSearch("NA"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseInstitutionLocationIdForProviderSearchRejectsInvalidValue() {
		ProviderResource.parseInstitutionLocationIdForProviderSearch("invalid");
	}
}
