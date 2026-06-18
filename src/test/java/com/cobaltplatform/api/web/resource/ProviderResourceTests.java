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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.response.ClinicApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.AppointmentBookingLevel.AppointmentBookingLevelId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.JsonMapper.MappingNullability;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderResourceTests {
	@Test
	public void clinicReturnsClinicForCurrentInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID clinicId = UUID.fromString("ab629384-400a-4688-8465-04636ec2eaa2");

			setBookingV2Enabled(database, true);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.clinic(clinicId);
				ClinicApiResponse clinic = responseModelValue(response, "clinic");

				assertEquals(200, response.status());
				assertEquals(clinicId, clinic.getClinicId());
				assertEquals(InstitutionId.COBALT, clinic.getInstitutionId());
			});
		});
	}

	@Test
	public void providerIncludesBiographicalContactAndModalityDetails() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID providerId = UUID.randomUUID();
			UUID clinicId = UUID.randomUUID();
			UUID providerAddressId = UUID.randomUUID();
			UUID institutionLocationId = UUID.randomUUID();
			String providerUrlName = "detail-provider-" + providerId;
			String providerDetailsHtml = "<section><h2>Provider Details</h2><p>Provider detail copy.</p></section>";

			database.execute("""
					INSERT INTO clinic (
					  clinic_id, description, treatment_description, institution_id, appointment_booking_level_id
					) VALUES (?, ?, ?, ?, ?)
					""", clinicId, "Detail Provider Clinic", "Provider treatment", InstitutionId.COBALT, AppointmentBookingLevelId.PROVIDER);
			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, title, email_address, url_name, image_url, locale, time_zone,
					  scheduling_system_id, videoconference_platform_id, description, bio_url, bio, phone_number,
					  display_phone_number_only_for_booking, details_html
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, InstitutionId.COBALT, "Detail Provider", "LCSW",
					"detail-provider-" + providerId + "@example.com", providerUrlName, "https://example.com/provider.png",
					"en-US", "America/New_York", "ACUITY", "SWITCHBOARD", "Provider description",
					"https://example.com/provider-bio", "Line one\nLine two", "+12155551212", false, providerDetailsHtml);
			database.execute("""
					INSERT INTO provider_clinic (
					  provider_clinic_id, provider_id, clinic_id, primary_clinic
					) VALUES (?, ?, ?, ?)
					""", UUID.randomUUID(), providerId, clinicId, true);
			database.execute("""
					INSERT INTO address (
					  address_id, postal_name, street_address_1, locality, region, postal_code, country_code, formatted_address
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", providerAddressId, "Provider Location Postal", "101 Provider Lane", "Philadelphia", "PA",
					"19104", "US", "101 Provider Lane, Philadelphia, PA 19104");
			database.execute("""
					INSERT INTO institution_location (
					  institution_location_id, institution_id, address_id, name, short_name, display_order,
					  phone_number, website_url, email_address
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", institutionLocationId, InstitutionId.COBALT, providerAddressId, "Detail Provider Office",
					"Provider Office", 1, "+12155551414", "https://example.com/provider-location",
					"provider-location@example.com");
			database.execute("""
					INSERT INTO provider_institution_location (
					  provider_location_id, provider_id, institution_location_id
					) VALUES (?, ?, ?)
					""", UUID.randomUUID(), providerId, institutionLocationId);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.provider(providerUrlName);
				ProviderApiResponse provider = responseModelValue(response, "provider");

				assertEquals(200, response.status());
				assertEquals(providerId, provider.getProviderId());
				assertEquals("Provider description", provider.getDescription());
				assertEquals("* Provider treatment", provider.getTreatmentDescription());
				assertEquals(providerDetailsHtml, provider.getDetailsHtml());
				assertEquals("https://example.com/provider-bio", provider.getBioUrl());
				assertEquals("https://example.com/provider-bio", provider.getWebsiteUrl());
				assertEquals("Line one<br/>Line two", provider.getBio());
				assertEquals("+12155551212", provider.getPhoneNumber());
				assertEquals("(215) 555-1212", provider.getPhoneNumberDescription());
				assertEquals("(215) 555-1212", provider.getFormattedPhoneNumber());
				assertFalse(provider.getDisplayPhoneNumberOnlyForBooking());
				assertEquals(1, provider.getLocations().size());
				InstitutionLocationApiResponse location = provider.getLocations().get(0);
				assertEquals(institutionLocationId, location.getInstitutionLocationId());
				assertEquals("Detail Provider Office", location.getName());
				assertEquals("+12155551414", location.getPhoneNumber());
				assertEquals("(215) 555-1414", location.getFormattedPhoneNumber());
				assertEquals("https://example.com/provider-location", location.getWebsiteUrl());
				assertEquals("provider-location@example.com", location.getEmailAddress());
				assertNotNull(location.getAddress());
				assertEquals(providerAddressId, location.getAddress().getAddressId());
				assertEquals("101 Provider Lane", location.getAddress().getStreetAddress1());
				assertEquals("101 Provider Lane, Philadelphia, PA 19104", location.getAddress().getFormattedAddress());
				assertEquals(2, provider.getSupportedAppointmentModalities().size());
				assertEquals(ProviderAppointmentModalityId.PHONE,
						provider.getSupportedAppointmentModalities().get(0).getAppointmentModalityId());
				assertEquals(ProviderAppointmentModalityId.VIRTUAL,
						provider.getSupportedAppointmentModalities().get(1).getAppointmentModalityId());
			});
		});
	}

	@Test
	public void clinicIncludesContactAndModalityDetails() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID clinicId = UUID.randomUUID();
			UUID phoneProviderId = UUID.randomUUID();
			UUID virtualProviderId = UUID.randomUUID();
			UUID clinicAddressId = UUID.randomUUID();
			UUID institutionLocationId = UUID.randomUUID();
			String clinicDetailsHtml = "<section><h2>Clinic Details</h2><p>Clinic detail copy.</p></section>";

			setBookingV2Enabled(database, true);

			database.execute("""
					INSERT INTO clinic (
					  clinic_id, description, treatment_description, institution_id, appointment_booking_level_id,
					  phone_number, image_url, locale, website_url, details_html
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", clinicId, "Detail Clinic", "Clinic treatment", InstitutionId.COBALT, AppointmentBookingLevelId.CLINIC,
					"+12155551313", "https://example.com/clinic.png", "en-US", "https://example.com/detail-clinic",
					clinicDetailsHtml);
			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, email_address, url_name, locale, time_zone,
					  scheduling_system_id, videoconference_platform_id
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", phoneProviderId, InstitutionId.COBALT, "Phone Clinic Provider",
					"phone-clinic-provider-" + phoneProviderId + "@example.com", "phone-clinic-provider-" + phoneProviderId,
					"en-US", "America/New_York", "ACUITY", "TELEPHONE");
			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, email_address, url_name, locale, time_zone,
					  scheduling_system_id, videoconference_platform_id
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", virtualProviderId, InstitutionId.COBALT, "Virtual Clinic Provider",
					"virtual-clinic-provider-" + virtualProviderId + "@example.com", "virtual-clinic-provider-" + virtualProviderId,
					"en-US", "America/New_York", "ACUITY", "SWITCHBOARD");
			database.execute("""
					INSERT INTO provider_clinic (
					  provider_clinic_id, provider_id, clinic_id, primary_clinic
					) VALUES (?, ?, ?, ?)
					""", UUID.randomUUID(), phoneProviderId, clinicId, true);
			database.execute("""
					INSERT INTO provider_clinic (
					  provider_clinic_id, provider_id, clinic_id, primary_clinic
					) VALUES (?, ?, ?, ?)
					""", UUID.randomUUID(), virtualProviderId, clinicId, false);
			database.execute("""
					INSERT INTO address (
					  address_id, postal_name, street_address_1, locality, region, postal_code, country_code, formatted_address
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", clinicAddressId, "Clinic Location Postal", "202 Clinic Avenue", "Philadelphia", "PA",
					"19107", "US", "202 Clinic Avenue, Philadelphia, PA 19107");
			database.execute("""
					INSERT INTO institution_location (
					  institution_location_id, institution_id, address_id, name, short_name, display_order,
					  phone_number, website_url, email_address
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", institutionLocationId, InstitutionId.COBALT, clinicAddressId, "Detail Clinic Front Desk",
					"Clinic Front Desk", 1, "+12155551616", "https://example.com/detail-clinic-location",
					"detail-clinic-location@example.com");
			database.execute("""
					INSERT INTO provider_institution_location (
					  provider_location_id, provider_id, institution_location_id
					) VALUES (?, ?, ?)
					""", UUID.randomUUID(), phoneProviderId, institutionLocationId);
			database.execute("""
					INSERT INTO provider_institution_location (
					  provider_location_id, provider_id, institution_location_id
					) VALUES (?, ?, ?)
					""", UUID.randomUUID(), virtualProviderId, institutionLocationId);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.clinic(clinicId);
				ClinicApiResponse clinic = responseModelValue(response, "clinic");

				assertEquals(200, response.status());
				assertEquals(clinicId, clinic.getClinicId());
				assertEquals("Detail Clinic", clinic.getName());
				assertEquals("Detail Clinic", clinic.getDescription());
				assertEquals("Clinic treatment", clinic.getTreatmentDescription());
				assertEquals(clinicDetailsHtml, clinic.getDetailsHtml());
				assertEquals(AppointmentBookingLevelId.CLINIC, clinic.getAppointmentBookingLevelId());
				assertEquals("+12155551313", clinic.getPhoneNumber());
				assertEquals("(215) 555-1313", clinic.getPhoneNumberDescription());
				assertEquals("(215) 555-1313", clinic.getFormattedPhoneNumber());
				assertEquals("https://example.com/clinic.png", clinic.getImageUrl());
				assertEquals("https://example.com/detail-clinic", clinic.getWebsiteUrl());
				assertEquals(1, clinic.getLocations().size());
				InstitutionLocationApiResponse location = clinic.getLocations().get(0);
				assertEquals(institutionLocationId, location.getInstitutionLocationId());
				assertEquals("Detail Clinic Front Desk", location.getName());
				assertEquals("+12155551616", location.getPhoneNumber());
				assertEquals("(215) 555-1616", location.getFormattedPhoneNumber());
				assertEquals("https://example.com/detail-clinic-location", location.getWebsiteUrl());
				assertEquals("detail-clinic-location@example.com", location.getEmailAddress());
				assertNotNull(location.getAddress());
				assertEquals(clinicAddressId, location.getAddress().getAddressId());
				assertEquals("202 Clinic Avenue", location.getAddress().getStreetAddress1());
				assertEquals("202 Clinic Avenue, Philadelphia, PA 19107", location.getAddress().getFormattedAddress());
				assertEquals(2, clinic.getSupportedAppointmentModalities().size());
				assertEquals(ProviderAppointmentModalityId.PHONE,
						clinic.getSupportedAppointmentModalities().get(0).getAppointmentModalityId());
				assertEquals(ProviderAppointmentModalityId.VIRTUAL,
						clinic.getSupportedAppointmentModalities().get(1).getAppointmentModalityId());
			});
		});
	}

	@Test
	public void providersListOmitsDetailsHtml() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID providerId = UUID.randomUUID();

			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, email_address, url_name, locale, time_zone,
					  scheduling_system_id, videoconference_platform_id, details_html
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, InstitutionId.COBALT, "List Details Provider",
					"list-details-provider-" + providerId + "@example.com", "list-details-provider-" + providerId,
					"en-US", "America/New_York", "ACUITY", "TELEPHONE",
					"<section><h2>Hidden List Details</h2></section>");

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.providers();
				List<ProviderApiResponse> providers = responseModelValue(response, "providers");
				ProviderApiResponse provider = providers.stream()
						.filter(providerResponse -> providerId.equals(providerResponse.getProviderId()))
						.findFirst()
						.get();
				Map<String, Object> serializedProvider = jsonMapperExcludingNulls().toMap(provider);

				assertFalse(serializedProvider.containsKey("detailsHtml"));
			});
		});
	}

	@Test
	public void autocompleteClinicsIncludeWebsiteAndLocations() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID clinicId = UUID.randomUUID();
			UUID providerId = UUID.randomUUID();
			UUID addressId = UUID.randomUUID();
			UUID institutionLocationId = UUID.randomUUID();
			String clinicDescription = "Autocomplete Clinic " + clinicId;

			database.execute("""
					INSERT INTO clinic (
					  clinic_id, description, treatment_description, institution_id, appointment_booking_level_id,
					  phone_number, locale, website_url
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", clinicId, clinicDescription, "Autocomplete clinic treatment", InstitutionId.COBALT,
					AppointmentBookingLevelId.CLINIC, "+12155551717", "en-US", "https://example.com/autocomplete-clinic");
			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, email_address, url_name, locale, time_zone,
					  scheduling_system_id, videoconference_platform_id
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, InstitutionId.COBALT, "Autocomplete Clinic Provider",
					"autocomplete-clinic-provider-" + providerId + "@example.com",
					"autocomplete-clinic-provider-" + providerId, "en-US", "America/New_York", "ACUITY", "TELEPHONE");
			database.execute("""
					INSERT INTO provider_clinic (
					  provider_clinic_id, provider_id, clinic_id, primary_clinic
					) VALUES (?, ?, ?, ?)
					""", UUID.randomUUID(), providerId, clinicId, true);
			database.execute("""
					INSERT INTO address (
					  address_id, postal_name, street_address_1, locality, region, postal_code, country_code, formatted_address
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", addressId, "Autocomplete Location Postal", "303 Autocomplete Road", "Philadelphia", "PA",
					"19108", "US", "303 Autocomplete Road, Philadelphia, PA 19108");
			database.execute("""
					INSERT INTO institution_location (
					  institution_location_id, institution_id, address_id, name, short_name, display_order,
					  phone_number, website_url, email_address
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", institutionLocationId, InstitutionId.COBALT, addressId, "Autocomplete Clinic Desk",
					"Autocomplete Desk", 1, "+12155551818", "https://example.com/autocomplete-location",
					"autocomplete-location@example.com");
			database.execute("""
					INSERT INTO provider_institution_location (
					  provider_location_id, provider_id, institution_location_id
					) VALUES (?, ?, ?)
					""", UUID.randomUUID(), providerId, institutionLocationId);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.autocompleteProviders(Optional.of(clinicDescription));
				List<ClinicApiResponse> clinics = responseModelValue(response, "clinics");

				assertEquals(200, response.status());
				assertEquals(1, clinics.size());

				ClinicApiResponse clinic = clinics.get(0);
				assertEquals(clinicId, clinic.getClinicId());
				assertEquals("https://example.com/autocomplete-clinic", clinic.getWebsiteUrl());
				assertEquals(1, clinic.getLocations().size());
				assertFalse(jsonMapperExcludingNulls().toMap(clinic).containsKey("detailsHtml"));

				InstitutionLocationApiResponse location = clinic.getLocations().get(0);
				assertEquals(institutionLocationId, location.getInstitutionLocationId());
				assertEquals("Autocomplete Clinic Desk", location.getName());
				assertEquals("(215) 555-1818", location.getFormattedPhoneNumber());
				assertNotNull(location.getAddress());
				assertEquals("303 Autocomplete Road, Philadelphia, PA 19108", location.getAddress().getFormattedAddress());
			});
		});
	}

	@Test
	public void providerAndClinicReturnEmptyLocationListsWhenNoneAvailable() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID providerId = UUID.randomUUID();
			UUID clinicId = UUID.randomUUID();
			String providerUrlName = "empty-location-provider-" + providerId;

			setBookingV2Enabled(database, true);

			database.execute("""
					INSERT INTO clinic (
					  clinic_id, description, treatment_description, institution_id, appointment_booking_level_id
					) VALUES (?, ?, ?, ?, ?)
					""", clinicId, "Empty Location Clinic", "Empty location clinic treatment", InstitutionId.COBALT,
					AppointmentBookingLevelId.PROVIDER);
			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, email_address, url_name, locale, time_zone,
					  scheduling_system_id, videoconference_platform_id
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, InstitutionId.COBALT, "Empty Location Provider",
					"empty-location-provider-" + providerId + "@example.com", providerUrlName, "en-US",
					"America/New_York", "ACUITY", "TELEPHONE");
			database.execute("""
					INSERT INTO provider_clinic (
					  provider_clinic_id, provider_id, clinic_id, primary_clinic
					) VALUES (?, ?, ?, ?)
					""", UUID.randomUUID(), providerId, clinicId, true);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse providerResponse = providerResource.provider(providerUrlName);
				ProviderApiResponse provider = responseModelValue(providerResponse, "provider");

				assertEquals(200, providerResponse.status());
				assertNotNull(provider.getLocations());
				assertTrue(provider.getLocations().isEmpty());

				ApiResponse clinicResponse = providerResource.clinic(clinicId);
				ClinicApiResponse clinic = responseModelValue(clinicResponse, "clinic");

				assertEquals(200, clinicResponse.status());
				assertNotNull(clinic.getLocations());
				assertTrue(clinic.getLocations().isEmpty());
			});
		});
	}

	@Test(expected = NotFoundException.class)
	public void clinicRejectsMissingClinic() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			setBookingV2Enabled(database, true);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> providerResource.clinic(UUID.randomUUID()));
		});
	}

	@Test(expected = AuthorizationException.class)
	public void clinicRejectsClinicFromAnotherInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID clinicId = UUID.randomUUID();

			setBookingV2Enabled(database, true);

			database.execute("""
					INSERT INTO clinic (clinic_id, description, institution_id)
					VALUES (?, ?, ?)
					""", clinicId, "Cross Institution Clinic", InstitutionId.COBALT_IC);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> providerResource.clinic(clinicId));
		});
	}

	@Test(expected = NotFoundException.class)
	public void clinicReturnsNotFoundWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			setBookingV2Enabled(database, false);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> providerResource.clinic(UUID.randomUUID()));
		});
	}

	@Test(expected = NotFoundException.class)
	public void searchProvidersReturnsNotFoundWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			setBookingV2Enabled(database, false);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> providerResource.searchProviders(Optional.of(FeatureId.THERAPY), Optional.of("na")));
		});
	}

	@Test(expected = NotFoundException.class)
	public void providerAvailabilityReturnsNotFoundWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderAvailabilityResource providerAvailabilityResource = app.getInjector().getInstance(ProviderAvailabilityResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			setBookingV2Enabled(database, false);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> providerAvailabilityResource.providerAvailability(UUID.randomUUID(), Optional.empty(), Optional.empty(),
							Optional.of("invalid-feature-id"), Optional.empty()));
		});
	}

	@Test(expected = NotFoundException.class)
	public void clinicAvailabilityReturnsNotFoundWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderAvailabilityResource providerAvailabilityResource = app.getInjector().getInstance(ProviderAvailabilityResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			setBookingV2Enabled(database, false);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> providerAvailabilityResource.clinicAvailability(UUID.randomUUID(), Optional.empty(), Optional.empty(),
							Optional.of("invalid-feature-id"), Optional.empty()));
		});
	}

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

	@SuppressWarnings("unchecked")
	private static <T> T responseModelValue(ApiResponse response,
																					String key) {
		Map<String, Object> model = (Map<String, Object>) response.model().get();
		return (T) model.get(key);
	}

	private static void setBookingV2Enabled(Database database,
																					boolean enabled) {
		database.execute("UPDATE institution SET booking_v2_enabled=? WHERE institution_id=?", enabled, InstitutionId.COBALT);
	}

	private static JsonMapper jsonMapperExcludingNulls() {
		return new JsonMapper.Builder()
				.mappingNullability(MappingNullability.EXCLUDE_NULLS)
				.build();
	}
}
