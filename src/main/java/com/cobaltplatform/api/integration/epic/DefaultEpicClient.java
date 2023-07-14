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

package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.epic.request.AppointmentBookFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.AppointmentFindFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.AppointmentSearchFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientDemographicsRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.PatientCreateRequest;
import com.cobaltplatform.api.integration.epic.request.PatientSearchRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.AppointmentBookFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.AppointmentFindFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.AppointmentSearchFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.CancelAppointmentResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientAppointmentsResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientDemographicsResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.PatientCreateResponse;
import com.cobaltplatform.api.integration.epic.response.PatientReadFhirR4Response;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.util.Normalizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Singleton
public class DefaultEpicClient implements EpicClient {
	@Nonnull
	private final EpicConfiguration epicConfiguration;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final DateTimeFormatter dateFormatterHyphens;
	@Nonnull
	private final DateTimeFormatter dateFormatterSlashes;
	@Nonnull
	private final DateTimeFormatter amPmTimeFormatter;
	@Nonnull
	private final DateTimeFormatter militaryTimeFormatter;
	@Nonnull
	private final Pattern phoneNumberPattern;
	@Nonnull
	private final Logger logger;

	public DefaultEpicClient(@Nonnull EpicConfiguration epicConfiguration) {
		requireNonNull(epicConfiguration);

		this.epicConfiguration = epicConfiguration;
		this.httpClient = createHttpClient(epicConfiguration);
		this.gson = EpicUtilities.defaultGson();
		this.normalizer = new Normalizer();
		this.dateFormatterHyphens = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US); // e.g. 1987-04-21
		this.dateFormatterSlashes = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US); // e.g. 6/8/2020
		this.amPmTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US); // e.g. "8:00 AM"
		this.militaryTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US); // e.g. "08:00"
		this.phoneNumberPattern = Pattern.compile("^\\d{3}-\\d{3}-\\d{4}$");
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@Override
	public Optional<PatientReadFhirR4Response> patientReadFhirR4(@Nullable String patientId) {
		patientId = trimToNull(patientId);

		if (patientId == null)
			return Optional.empty();

		HttpMethod httpMethod = HttpMethod.GET;
		String url = format("api/FHIR/R4/Patient/%s", patientId);

		// TODO: handle "not found" case
		//
		// {
		//    "resourceType": "OperationOutcome",
		//    "issue": [
		//        {
		//            "severity": "fatal",
		//            "code": "not-found",
		//            "details": {
		//                "coding": [
		//                    {
		//                        "system": "urn:oid:1.2.840.114350.1.13.87.3.7.2.657369",
		//                        "code": "59144",
		//                        "display": "The reference provided was not found."
		//                    }
		//                ],
		//                "text": "The reference provided was not found."
		//            },
		//            "diagnostics": "Invalid FHIR ID provided",
		//            "location": [
		//                "/f:id"
		//            ],
		//            "expression": [
		//                "id"
		//            ]
		//        }
		//    ]
		// }

		Function<String, Optional<PatientReadFhirR4Response>> responseBodyMapper = (responseBody) -> {
			PatientReadFhirR4Response response = getGson().fromJson(responseBody, PatientReadFhirR4Response.class);
			response.setRawJson(responseBody.trim());

			return Optional.of(response);
		};

		ApiCall<Optional<PatientReadFhirR4Response>> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public AppointmentFindFhirStu3Response appointmentFindFhirStu3(@Nonnull AppointmentFindFhirStu3Request request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/FHIR/STU3/Appointment/$find";

		String patient = trimToNull(request.getPatient());
		Instant startTime = request.getStartTime().truncatedTo(ChronoUnit.SECONDS);
		Instant endTime = request.getEndTime().truncatedTo(ChronoUnit.SECONDS);
		List<AppointmentFindFhirStu3Request.Coding> serviceTypes = request.getServiceTypes() == null ? Collections.emptyList() : request.getServiceTypes();
		String serviceTypesText = trimToNull(request.getServiceTypesText());
		List<AppointmentFindFhirStu3Request.Coding> indications = request.getIndications() == null ? Collections.emptyList() : request.getIndications();
		String indicationsText = trimToNull(request.getIndicationsText());
		List<AppointmentFindFhirStu3Request.Coding> specialities = request.getSpecialities() == null ? Collections.emptyList() : request.getSpecialities();
		String specialitiesText = trimToNull(request.getSpecialitiesText());
		String locationReference = trimToNull(request.getLocationReference());
		List<AppointmentFindFhirStu3Request.ValueTiming> timesOfDay = request.getTimesOfDay() == null ? Collections.emptyList() : request.getTimesOfDay();

		// e.g. 02:00:00
		DateTimeFormatter timeOfDayFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);

		List<Map<String, Object>> parameters = new ArrayList<>();

		// The official documentation at https://fhir.epic.com/Specifications?api=840 seems incorrect - trying this instead
		if (patient != null) {
			Map<String, Object> parameter = new HashMap();
			parameter.put("name", "patient");
			parameter.put("resource", Map.of(
					"resourceType", "patient",
					"identifier", List.of(
							Map.of(
									"value", patient
							)
					)));

			parameters.add(parameter);
		}

		// {
		//    "name": "startTime",
		//    "valueDateTime": "2018-07-30T18:15:50Z"
		// }
		if (startTime != null) {
			Map<String, Object> parameter = new HashMap();
			parameter.put("name", "startTime");
			parameter.put("valueDateTime", DateTimeFormatter.ISO_INSTANT.format(startTime));
			parameters.add(parameter);
		}

		// {
		//    "name": "endTime",
		//    "valueDateTime": "2018-07-30T18:15:50Z"
		// }
		if (endTime != null) {
			Map<String, Object> parameter = new HashMap();
			parameter.put("name", "endTime");
			parameter.put("valueDateTime", DateTimeFormatter.ISO_INSTANT.format(endTime));
			parameters.add(parameter);
		}

		// {
		//    "name": "serviceType",
		//    "valueCodeableConcept": {
		//        "coding": [
		//            {
		//                "system": "urn:oid:1.2.840.114350.1.13.861.1.7.3.808267.11",
		//                "code": "40111223",
		//                "display": "CDS Office Visit"
		//            },
		//            {
		//                "system": "DNPRC6",
		//                "code": "159",
		//                "display": "CDS Office Visit"
		//            }
		//        ],
		//        "text": "CDS Office Visit"
		//    }
		// }
		if (serviceTypesText != null || serviceTypes.size() > 0) {
			List<AppointmentFindFhirStu3Request.Coding> codings = serviceTypes.stream()
					.map(serviceType -> {
						AppointmentFindFhirStu3Request.Coding coding = new AppointmentFindFhirStu3Request.Coding();
						coding.setDisplay(trimToNull(serviceType.getDisplay()));
						coding.setCode(trimToNull(serviceType.getCode()));
						coding.setSystem(trimToNull(serviceType.getSystem()));

						return coding;
					})
					.collect(Collectors.toList());

			Map<String, Object> valueCodeableConceptJson = new HashMap<>();
			valueCodeableConceptJson.put("text", serviceTypesText);
			valueCodeableConceptJson.put("coding", codings);

			Map<String, Object> parameter = new HashMap<>();
			parameter.put("name", "serviceType");
			parameter.put("valueCodeableConcept", valueCodeableConceptJson);

			parameters.add(parameter);
		}

		// {
		//    "name": "indications",
		//    "valueCodeableConcept": {
		//        "coding": [
		//            {
		//                "system": "urn:oid:2.16.840.1.113883.6.96",
		//                "code": "46866001",
		//                "display": "Fracture of lower limb (disorder)"
		//            },
		//            {
		//                "system": "urn:oid:2.16.840.1.113883.6.90",
		//                "code": "S82.90XA",
		//                "display": "Broken leg"
		//            },
		//            {
		//                "system": "urn:oid:1.2.840.114350.1.13.861.1.7.2.696871",
		//                "code": "121346631",
		//                "display": "Broken leg"
		//            }
		//        ],
		//        "text": "Broken leg"
		//    }
		// }
		if (indicationsText != null || indications.size() > 0) {
			List<AppointmentFindFhirStu3Request.Coding> codings = indications.stream()
					.map(indication -> {
						AppointmentFindFhirStu3Request.Coding coding = new AppointmentFindFhirStu3Request.Coding();
						coding.setDisplay(trimToNull(indication.getDisplay()));
						coding.setCode(trimToNull(indication.getCode()));
						coding.setSystem(trimToNull(indication.getSystem()));

						return coding;
					})
					.collect(Collectors.toList());

			Map<String, Object> valueCodeableConceptJson = new HashMap<>();
			valueCodeableConceptJson.put("text", indicationsText);
			valueCodeableConceptJson.put("coding", codings);

			Map<String, Object> parameter = new HashMap<>();
			parameter.put("name", "indications");
			parameter.put("valueCodeableConcept", valueCodeableConceptJson);

			parameters.add(parameter);
		}

		// {
		//    "name": "specialties",
		//    "valueCodeableConcept": {
		//        "coding": [
		//            {
		//                "system": "urn:oid:1.2.840.114350.1.72.1.7.7.10.688867.4150",
		//                "code": "20",
		//                "display": "Gastroenterology"
		//            },
		//            {
		//                "system": "urn:oid:1.2.840.114350.1.13.861.1.7.10.686980.110",
		//                "code": "10",
		//                "display": "Gastroenterology"
		//            }
		//        ],
		//        "text": "Gastroenterology"
		//    }
		// }
		if (specialitiesText != null || specialities.size() > 0) {
			List<AppointmentFindFhirStu3Request.Coding> codings = specialities.stream()
					.map(indication -> {
						AppointmentFindFhirStu3Request.Coding coding = new AppointmentFindFhirStu3Request.Coding();
						coding.setDisplay(trimToNull(indication.getDisplay()));
						coding.setCode(trimToNull(indication.getCode()));
						coding.setSystem(trimToNull(indication.getSystem()));

						return coding;
					})
					.collect(Collectors.toList());

			Map<String, Object> valueCodeableConceptJson = new HashMap<>();
			valueCodeableConceptJson.put("text", specialitiesText);
			valueCodeableConceptJson.put("coding", codings);

			Map<String, Object> parameter = new HashMap<>();
			parameter.put("name", "indications");
			parameter.put("valueCodeableConcept", valueCodeableConceptJson);

			parameters.add(parameter);
		}

		// {
		//    "name": "location-reference",
		//    "valueReference": {
		//        "reference": "https://hostname/instance/api/FHIR/STU3/Location/eULujY-VWnFz-tbLqZ39RjA3}"
		//    }
		// }
		if (locationReference != null) {
			Map<String, Object> parameter = new HashMap<>();
			parameter.put("name", "location-reference");
			parameter.put("valueReference", Map.of("reference", locationReference));

			parameters.add(parameter);
		}

		// {
		//    "name": "time-of-day",
		//    "valueTiming": {
		//        "repeat": {
		//            "duration": "10",
		//            "durationUnit": "min",
		//            "dayOfWeek": [
		//                "thu",
		//                "tue"
		//            ],
		//            "timeOfDay": [
		//                "01:00:00"
		//            ]
		//        }
		//    }
		// }
		if (timesOfDay.size() > 0) {
			for (AppointmentFindFhirStu3Request.ValueTiming valueTiming : timesOfDay) {
				Map<String, Object> valueTimingJson = new HashMap<>();

				if (valueTiming.getRepeat() != null) {
					List<AppointmentFindFhirStu3Request.DayOfWeek> repeatDaysOfWeek = valueTiming.getRepeat().getDaysOfWeek();
					List<LocalTime> repeatTimesOfDay = valueTiming.getRepeat().getTimesOfDay();
					Integer durationInMinutes = valueTiming.getRepeat().getDurationInMinutes();

					Map<String, Object> repeatJson = new HashMap<>();

					if (durationInMinutes != null) {
						repeatJson.put("duration", String.valueOf(durationInMinutes));
						repeatJson.put("durationUnit", "min");
					}

					if (repeatDaysOfWeek != null)
						repeatJson.put("dayOfWeek", repeatDaysOfWeek.stream()
								.map(dayOfWeek -> dayOfWeek.getEpicValue())
								.collect(Collectors.toList()));


					if (repeatTimesOfDay != null)
						repeatJson.put("timeOfDay", repeatTimesOfDay.stream()
								.map(timeOfDay -> timeOfDay.format(timeOfDayFormatter))
								.collect(Collectors.toList()));

					valueTimingJson.put("repeat", repeatJson);
				}

				Map<String, Object> parameter = new HashMap<>();
				parameter.put("name", "time-of-day");
				parameter.put("valueTiming", valueTimingJson);

				parameters.add(parameter);
			}
		}

		Map<String, Object> requestBodyJson = new HashMap<>();
		requestBodyJson.put("resourceType", "Parameters");
		requestBodyJson.put("parameter", parameters);

		String requestBody = getGson().toJson(requestBodyJson);

		// TODO: handle "not found" case
		//
		// {
		//    "resourceType": "Bundle",
		//    "type": "searchset",
		//    "total": 0,
		//    "link": [
		//        {
		//            "relation": "self",
		//            "url": "https://BASE_URL/api/FHIR/STU3/Appointment/$find"
		//        }
		//    ],
		//    "entry": [
		//        {
		//            "fullUrl": "urn:uuid:00000000-0196-b030-7276-5ae35f23e392",
		//            "resource": {
		//                "resourceType": "OperationOutcome",
		//                "issue": [
		//                    {
		//                        "severity": "warning",
		//                        "code": "not-found",
		//                        "details": {
		//                            "coding": [
		//                                {
		//                                    "system": "urn:oid:1.2.840.114350.1.13.236.2.7.2.657369",
		//                                    "code": "59200",
		//                                    "display": "The Cadence Decision Tree was unable to determine a Visit Type based on the supplied parameters. Check the request parameters and decision tree build."
		//                                }
		//                            ],
		//                            "text": "The Cadence Decision Tree was unable to determine a Visit Type based on the supplied parameters. Check the request parameters and decision tree build."
		//                        },
		//                        "diagnostics": "Unable to determine a Visit Type to use for scheduling."
		//                    },
		//                    {
		//                        "severity": "warning",
		//                        "code": "processing",
		//                        "details": {
		//                            "coding": [
		//                                {
		//                                    "system": "urn:oid:1.2.840.114350.1.13.236.2.7.2.657369",
		//                                    "code": "4101",
		//                                    "display": "Resource request returns no results."
		//                                }
		//                            ],
		//                            "text": "Resource request returns no results."
		//                        }
		//                    }
		//                ]
		//            },
		//            "search": {
		//                "mode": "outcome"
		//            }
		//        }
		//    ]
		// }

		Function<String, AppointmentFindFhirStu3Response> responseBodyMapper = (responseBody) -> {
			AppointmentFindFhirStu3Response response = getGson().fromJson(responseBody, AppointmentFindFhirStu3Response.class);
			response.setRawJson(responseBody.trim());

			return response;
		};

		ApiCall<AppointmentFindFhirStu3Response> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(requestBody)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public AppointmentBookFhirStu3Response appointmentBookFhirStu3(@Nonnull AppointmentBookFhirStu3Request request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/FHIR/STU3/Appointment/$book";

		String patient = trimToNull(request.getPatient());
		String appointment = trimToNull(request.getAppointment());
		String appointmentNote = trimToNull(request.getAppointmentNote());

		// {
		//   "resourceType":"Parameters",
		//   "parameter":[
		//      {
		//         "name":"patient",
		//         "valueIdentifier":{
		//            "value":"efvHwbc1k1CQ9XjM1zvvefQ3"
		//         }
		//      },
		//      {
		//         "name":"appointment",
		//         "valueIdentifier":{
		//            "value":"ezu6MfS.FpOXrHAn1eJHczv4LlH.fMIwtdkA8rsm-Yfu96eUh91EBd0UN9BZx7kbB3"
		//         }
		//      },
		//      {
		//         "name":"appointmentNote",
		//         "valueString":"Note text containing info related to the appointment."
		//      }
		//   ]
		// }

		List<Map<String, Object>> parameters = new ArrayList<>();

		if (patient != null) {
			Map<String, Object> parameter = new HashMap();
			parameter.put("name", "patient");
			parameter.put("valueIdentifier", Map.of("value", patient));
			parameters.add(parameter);
		}

		if (appointment != null) {
			Map<String, Object> parameter = new HashMap();
			parameter.put("name", "appointment");
			parameter.put("valueIdentifier", Map.of("value", appointment));
			parameters.add(parameter);
		}

		if (appointmentNote != null) {
			Map<String, Object> parameter = new HashMap();
			parameter.put("name", "appointmentNote");
			parameter.put("valueIdentifier", Map.of("value", appointmentNote));
			parameters.add(parameter);
		}

		Map<String, Object> requestBodyJson = new HashMap<>();
		requestBodyJson.put("resourceType", "Parameters");
		requestBodyJson.put("parameter", parameters);

		String requestBody = getGson().toJson(requestBodyJson);

		// TODO: handle "not found" case
		//
		// {
		//    "resourceType": "OperationOutcome",
		//    "issue": [
		//        {
		//            "severity": "fatal",
		//            "code": "not-found",
		//            "details": {
		//                "coding": [
		//                    {
		//                        "system": "urn:oid:1.2.840.114350.1.13.236.2.7.2.657369",
		//                        "code": "59144",
		//                        "display": "The reference provided was not found."
		//                    }
		//                ],
		//                "text": "The reference provided was not found."
		//            },
		//            "diagnostics": "invalid appointment ID",
		//            "location": [
		//                "/f:appointment"
		//            ],
		//            "expression": [
		//                "appointment"
		//            ]
		//        }
		//    ]
		// }

		Function<String, AppointmentBookFhirStu3Response> responseBodyMapper = (responseBody) -> {
			AppointmentBookFhirStu3Response response = getGson().fromJson(responseBody, AppointmentBookFhirStu3Response.class);
			response.setRawJson(responseBody.trim());

			return response;
		};

		ApiCall<AppointmentBookFhirStu3Response> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(requestBody)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public AppointmentSearchFhirStu3Response appointmentSearchFhirStu3(@Nonnull AppointmentSearchFhirStu3Request request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.GET;
		String url = "api/FHIR/STU3/Appointment";

		String date = request.getDate() == null ? null : request.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US));
		String patient = trimToNull(request.getPatient());
		String identifier = trimToNull(request.getIdentifier());
		String status = request.getStatus() == null ? null : request.getStatus().getEpicValue();

		Map<String, Object> queryParameters = new HashMap<>();
		queryParameters.put("date", date);
		queryParameters.put("patient", patient);
		queryParameters.put("identifier", identifier);
		queryParameters.put("status", status);

		// TODO: handle "not found" case:
		//
		// {
		//    "resourceType": "Bundle",
		//    "type": "searchset",
		//    "total": 0,
		//    "link": [
		//        {
		//            "relation": "self",
		//            "url": "https://BASE_URL/api/FHIR/STU3/Appointment?date=2023-03-15&identifier=&patient=XXXXXX&status=booked"
		//        }
		//    ],
		//    "entry": [
		//        {
		//            "fullUrl": "urn:uuid:00000000-0196-744b-7276-5ae35f23e392",
		//            "resource": {
		//                "resourceType": "OperationOutcome",
		//                "issue": [
		//                    {
		//                        "severity": "warning",
		//                        "code": "processing",
		//                        "details": {
		//                            "coding": [
		//                                {
		//                                    "system": "urn:oid:1.2.840.114350.1.13.236.2.7.2.657369",
		//                                    "code": "4101",
		//                                    "display": "Resource request returns no results."
		//                                }
		//                            ],
		//                            "text": "Resource request returns no results."
		//                        }
		//                    }
		//                ]
		//            },
		//            "search": {
		//                "mode": "outcome"
		//            }
		//        }
		//    ]
		// }

		Function<String, AppointmentSearchFhirStu3Response> responseBodyMapper = (responseBody) -> {
			AppointmentSearchFhirStu3Response response = getGson().fromJson(responseBody, AppointmentSearchFhirStu3Response.class);
			response.setRawJson(responseBody.trim());

			return response;
		};

		ApiCall<AppointmentSearchFhirStu3Response> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.queryParameters(queryParameters)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public PatientSearchResponse performPatientSearch(@Nonnull PatientSearchRequest request) {
		requireNonNull(request);

		String identifier = trimToNull(request.getIdentifier());
		String family = trimToNull(request.getFamily());
		String given = trimToNull(request.getGiven());
		LocalDate birthdate = request.getBirthdate();
		PatientSearchRequest.Gender gender = request.getGender();
		String telecom = trimToNull(request.getTelecom());

		Map<String, Object> queryParameters = new HashMap<>();

		// Identifier trumps everything else if provided
		if (identifier != null) {
			queryParameters.put("identifier", identifier);
		} else {
			if (family != null)
				queryParameters.put("family", family);

			if (given != null)
				queryParameters.put("given", given);

			if (birthdate != null)
				queryParameters.put("birthdate", formatDateWithHyphens(birthdate));

			if (gender != null)
				queryParameters.put("gender", gender.epicValue());

			if (telecom != null)
				queryParameters.put("telecom", formatPhoneNumber(telecom));
		}

		HttpMethod httpMethod = HttpMethod.GET;
		String url = "api/FHIR/DSTU2/Patient";
		Function<String, PatientSearchResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, PatientSearchResponse.class);

		ApiCall<PatientSearchResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.queryParameters(queryParameters)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public GetPatientDemographicsResponse performGetPatientDemographics(@Nonnull GetPatientDemographicsRequest request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/epic/2015/Common/Patient/GetPatientDemographics/Patient/Demographics";
		Function<String, GetPatientDemographicsResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, GetPatientDemographicsResponse.class);

		ApiCall<GetPatientDemographicsResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(getGson().toJson(request))
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/epic/2012/Scheduling/Provider/GETPROVIDERSCHEDULE/Schedule";
		Function<String, GetProviderScheduleResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, GetProviderScheduleResponse.class);

		Map<String, Object> queryParameters = new HashMap<String, Object>() {{
			put("ProviderID", request.getProviderID());
			put("ProviderIDType", request.getProviderIDType());
			put("DepartmentID", request.getDepartmentID());
			put("DepartmentIDType", request.getDepartmentIDType());
			put("UserID", request.getUserID());
			put("UserIDType", request.getUserIDType());
			put("VisitTypeID", request.getVisitTypeID());
			put("VisitTypeIDType", request.getVisitTypeIDType());
			put("Date", formatDateWithSlashes(request.getDate()));
		}};

		ApiCall<GetProviderScheduleResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.queryParameters(queryParameters)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public GetPatientAppointmentsResponse performGetPatientAppointments(@Nonnull GetPatientAppointmentsRequest request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments";
		Function<String, GetPatientAppointmentsResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, GetPatientAppointmentsResponse.class);

		ApiCall<GetPatientAppointmentsResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(getGson().toJson(request))
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public PatientCreateResponse performPatientCreate(@Nonnull PatientCreateRequest request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/epic/2012/EMPI/External/PatientCreate/Patient/Create";
		Function<String, PatientCreateResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, PatientCreateResponse.class);

		ApiCall<PatientCreateResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(getGson().toJson(request))
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(@Nonnull ScheduleAppointmentWithInsuranceRequest request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/epic/2014/PatientAccess/External/ScheduleAppointmentWithInsurance/Scheduling/Open/ScheduleWithInsurance";
		Function<String, ScheduleAppointmentWithInsuranceResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, ScheduleAppointmentWithInsuranceResponse.class);

		ApiCall<ScheduleAppointmentWithInsuranceResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(getGson().toJson(request))
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public CancelAppointmentResponse performCancelAppointment(@Nonnull CancelAppointmentRequest request) {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "api/epic/2014/PatientAccess/External/CancelAppointment/Scheduling/Cancel";
		Function<String, CancelAppointmentResponse> responseBodyMapper = (responseBody) -> getGson().fromJson(responseBody, CancelAppointmentResponse.class);

		ApiCall<CancelAppointmentResponse> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(getGson().toJson(request))
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public LocalDate parseDateWithHyphens(@Nonnull String date) {
		requireNonNull(date);
		return LocalDate.parse(date, getDateFormatterHyphens());
	}

	@Nonnull
	@Override
	public String formatDateWithHyphens(@Nonnull LocalDate date) {
		requireNonNull(date);
		return getDateFormatterHyphens().format(date);
	}

	@Nonnull
	@Override
	public LocalDate parseDateWithSlashes(@Nonnull String date) {
		requireNonNull(date);
		return LocalDate.parse(date, getDateFormatterSlashes());
	}

	@Nonnull
	@Override
	public String formatDateWithSlashes(@Nonnull LocalDate date) {
		requireNonNull(date);
		return getDateFormatterSlashes().format(date);
	}

	@Nonnull
	@Override
	public String formatTimeInMilitary(@Nonnull LocalTime time) {
		requireNonNull(time);
		return getMilitaryTimeFormatter().format(time);
	}

	@Nonnull
	@Override
	public LocalTime parseTimeAmPm(@Nonnull String time) {
		requireNonNull(time);
		return LocalTime.parse(time.trim().toUpperCase(Locale.US), getAmPmTimeFormatter());
	}

	@Nonnull
	@Override
	public String formatPhoneNumber(@Nonnull String phoneNumber) {
		// Already formatted? Return immediately since some special placeholders will not be handled correctly by libphonenumber and our format is already OK.
		// Example: 999-999-9999
		Matcher matcher = getPhoneNumberPattern().matcher(phoneNumber);

		if (matcher.matches())
			return phoneNumber;

		String normalizedPhoneNumber = getNormalizer().normalizePhoneNumberToE164(phoneNumber, Locale.US).orElse(null);

		if (normalizedPhoneNumber == null)
			throw new EpicException(format("Unable to parse phone number '%s'", phoneNumber));

		// Format like "+12158889999" must be "215-888-9999"
		return format("%s-%s-%s", normalizedPhoneNumber.substring(2, 5), normalizedPhoneNumber.substring(5, 8), normalizedPhoneNumber.substring(8));
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull ApiCall<T> apiCall) {
		requireNonNull(apiCall);

		String finalUrl = format("%s/%s", getEpicConfiguration().getBaseUrl(), apiCall.getUrl());

		Map<String, Object> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");

		// 3 different flavors of EPIC authentication are supported
		MyChartAccessToken myChartAccessToken = getEpicConfiguration().getMyChartAccessToken().orElse(null);
		EpicBackendServiceAccessToken epicBackendServiceAccessToken = getEpicConfiguration().getEpicBackendServiceAccessToken().orElse(null);
		EpicEmpCredentials epicEmpCredentials = getEpicConfiguration().getEpicEmpCredentials().orElse(null);

		if (myChartAccessToken != null) {
			headers.put("Authorization", format("Bearer %s", myChartAccessToken.getAccessToken()));
		} else if (epicBackendServiceAccessToken != null) {
			headers.put("Authorization", format("Bearer %s", epicBackendServiceAccessToken.getAccessToken()));
		} else if (epicEmpCredentials != null) {
			String basicAuthCredentials = format("emp$%s:%s", epicEmpCredentials.getUsername(), epicEmpCredentials.getPassword());
			String encodedBasicAuthCredentials = Base64.getEncoder().encodeToString(basicAuthCredentials.getBytes(StandardCharsets.UTF_8));

			headers.put("Authorization", format("Basic %s", encodedBasicAuthCredentials));
			headers.put("Epic-Client-ID", epicEmpCredentials.getClientId());
			headers.put("Epic-User-ID", epicEmpCredentials.getUserId());
			headers.put("Epic-User-IDType", epicEmpCredentials.getUserIdType());
		}

		HttpRequest.Builder httpRequestBuilder = new HttpRequest.Builder(apiCall.getHttpMethod(), finalUrl)
				.headers(headers);

		if (apiCall.getQueryParameters().size() > 0)
			httpRequestBuilder.queryParameters(apiCall.getQueryParameters());

		String requestBody = apiCall.getRequestBody().orElse(null);

		if (requestBody != null)
			httpRequestBuilder.body(requestBody);

		if (apiCall.getHttpMethod() == HttpMethod.POST)
			httpRequestBuilder.contentType("application/json; charset=utf-8");

		HttpRequest httpRequest = httpRequestBuilder.build();

		String queryParametersDescription = apiCall.getQueryParameters().size() == 0 ? "[none]" : apiCall.getQueryParameters().toString();
		String requestBodyDescription = requestBody == null ? "[none]" : requestBody;

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);
			byte[] rawResponseBody = httpResponse.getBody().orElse(null);
			String responseBody = rawResponseBody == null ? null : new String(rawResponseBody, StandardCharsets.UTF_8);

			// TODO: parse messaging out into fields on EpicException for better error experience

			// {
			//   "Message":"An error has occurred.",
			//   "ExceptionMessage":"An error occurred while executing the command: NO-DATE-OF-BIRTH details: Date of birth is required.",
			//   "ExceptionType":"Epic.ServiceModel.Internal.ServiceCommandException",
			//   "StackTrace":"   at Epic.EMPI.Generated.Services.Epic_EMPI_ExternalController.v2012_PatientCreate(PATIENTCREATERequest theRequest)\r\n   at lambda_method(Closure , Object , Object[] )\r\n   at System.Web.Http.Controllers.ReflectedHttpActionDescriptor.ActionExecutor.<>c__DisplayClass10.<GetExecutor>b__9(Object instance, Object[] methodParameters)\r\n   at System.Web.Http.Controllers.ReflectedHttpActionDescriptor.ExecuteAsync(HttpControllerContext controllerContext, IDictionary`2 arguments, CancellationToken cancellationToken)\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Tracing.ITraceWriterExtensions.<TraceBeginEndAsyncCore>d__18`1.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Controllers.ApiControllerActionInvoker.<InvokeActionAsyncCore>d__0.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Tracing.ITraceWriterExtensions.<TraceBeginEndAsyncCore>d__18`1.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Filters.ActionFilterAttribute.<CallOnActionExecutedAsync>d__5.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Web.Http.Filters.ActionFilterAttribute.<CallOnActionExecutedAsync>d__5.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Filters.ActionFilterAttribute.<ExecuteActionFilterAsyncCore>d__0.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Controllers.ActionFilterResult.<ExecuteAsync>d__2.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Filters.AuthorizationFilterAttribute.<ExecuteAuthorizationFilterAsyncCore>d__2.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Controllers.AuthenticationFilterResult.<ExecuteAsync>d__0.MoveNext()\r\n--- End of stack trace from previous location where exception was thrown ---\r\n   at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()\r\n   at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)\r\n   at System.Web.Http.Controllers.ExceptionFilterResult.<ExecuteAsync>d__0.MoveNext()",
			//   "InnerException":{
			//      "Message":"An error has occurred.",
			//      "ExceptionMessage":"An error occurred while executing the command: NO-DATE-OF-BIRTH details: Date of birth is required.",
			//      "ExceptionType":"Epic.Core.Communication.EcfCommandException",
			//      "StackTrace":"   at Epic.Core.Communication.Internal.EcfConnection.HandleErrorPacket(Byte[] response, Int32 packetLength, Int32 startIndex, Command command, Int64 endTime, INetworkStream networkStream)\r\n   at Epic.Core.Communication.Internal.EcfConnection.BuildResponseFromPacket(Int32 packetLength, Byte[] response, Command command, INetworkStream networkStream, Int64 endTime, Boolean responseExpected, ProcessState& state, String& pauseMessage)\r\n   at Epic.Core.Communication.Internal.EcfConnection.Execute(Command command, String instrumentationHeader)\r\n   at Epic.Core.Communication.Connection.Execute(Command command, Int32 lockAcquireTimeout)\r\n   at Epic.Core.Communication.Command.Execute(Int32 lockAcquireTimeout, EventHandler`1 asyncExecuteCompletedHandler)\r\n   at Epic.EMPI.Generated.Services.Epic_EMPI_ExternalController.v2012_PatientCreate(PATIENTCREATERequest theRequest)"
			//   }
			//}

			if (httpResponse.getStatus() > 299)
				throw new EpicException(format("Bad HTTP response %d for EPIC endpoint %s %s with query params %s and request body %s. Response body was\n%s", httpResponse.getStatus(), httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody));

			try {
				return apiCall.getResponseBodyMapper().apply(responseBody);
			} catch (Exception e) {
				throw new EpicException(format("Unable to parse JSON for EPIC endpoint %s %s with query params %s and request body %s. Response body was\n%s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody), e);
			}
		} catch (IOException e) {
			throw new EpicException(format("Unable to call EPIC endpoint %s %s with query params %s and request body %s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription), e);
		}
	}

	@Nonnull
	protected HttpClient createHttpClient(@Nonnull EpicConfiguration epicConfiguration) {
		requireNonNull(epicConfiguration);
		return new DefaultHttpClient("com.cobaltplatform.api.integration.epic", epicConfiguration.getPermitUnsafeCerts());
	}

	@Nonnull
	protected String prettyPrintJson(@Nonnull String json) {
		Map<String, Object> map = getGson().fromJson(json, new TypeToken<Map<String, Object>>() {
		}.getType());
		return getGson().toJson(map);
	}

	@Nonnull
	protected EpicConfiguration getEpicConfiguration() {
		return this.epicConfiguration;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected DateTimeFormatter getDateFormatterHyphens() {
		return this.dateFormatterHyphens;
	}

	@Nonnull
	protected DateTimeFormatter getDateFormatterSlashes() {
		return this.dateFormatterSlashes;
	}

	@Nonnull
	protected DateTimeFormatter getAmPmTimeFormatter() {
		return this.amPmTimeFormatter;
	}

	@Nonnull
	protected DateTimeFormatter getMilitaryTimeFormatter() {
		return this.militaryTimeFormatter;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Pattern getPhoneNumberPattern() {
		return this.phoneNumberPattern;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@ThreadSafe
	protected static class ApiCall<T> {
		@Nonnull
		private final HttpMethod httpMethod;
		@Nonnull
		private final String url;
		@Nonnull
		private final Function<String, T> responseBodyMapper;
		@Nonnull
		private final Map<String, Object> queryParameters;
		@Nullable
		private final String requestBody;

		protected ApiCall(@Nonnull Builder<T> builder) {
			requireNonNull(builder);

			this.httpMethod = builder.httpMethod;
			this.url = builder.url;
			this.responseBodyMapper = builder.responseBodyMapper == null ? (responseBody) -> (T) responseBody : builder.responseBodyMapper;
			this.queryParameters = builder.queryParameters == null ? Collections.emptyMap() : builder.queryParameters;
			this.requestBody = builder.requestBody;
		}

		@Nonnull
		public HttpMethod getHttpMethod() {
			return this.httpMethod;
		}

		@Nonnull
		public String getUrl() {
			return this.url;
		}

		@Nonnull
		public Function<String, T> getResponseBodyMapper() {
			return this.responseBodyMapper;
		}

		@Nonnull
		public Map<String, Object> getQueryParameters() {
			return this.queryParameters;
		}

		@Nonnull
		public Optional<String> getRequestBody() {
			return Optional.ofNullable(this.requestBody);
		}

		@NotThreadSafe
		protected static class Builder<T> {
			@Nonnull
			private final HttpMethod httpMethod;
			@Nonnull
			private final String url;
			@Nonnull
			private final Function<String, T> responseBodyMapper;
			@Nullable
			private Map<String, Object> queryParameters;
			@Nullable
			private String requestBody;

			public Builder(@Nonnull HttpMethod httpMethod,
										 @Nonnull String url,
										 @Nonnull Function<String, T> responseBodyMapper) {
				requireNonNull(httpMethod);
				requireNonNull(url);
				requireNonNull(responseBodyMapper);

				this.httpMethod = httpMethod;
				this.url = url;
				this.responseBodyMapper = responseBodyMapper;
			}

			@Nonnull
			public Builder queryParameters(@Nullable Map<String, Object> queryParameters) {
				this.queryParameters = queryParameters;
				return this;
			}

			@Nonnull
			public Builder requestBody(@Nullable String requestBody) {
				this.requestBody = requestBody;
				return this;
			}

			@Nonnull
			public ApiCall<T> build() {
				return new ApiCall<>(this);
			}
		}
	}
}
