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
import com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientDemographicsRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.PatientCreateRequest;
import com.cobaltplatform.api.integration.epic.request.PatientSearchRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.CancelAppointmentResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientAppointmentsResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientDemographicsResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.PatientCreateResponse;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.util.Normalizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		this(epicConfiguration, null, null);
	}

	public DefaultEpicClient(@Nonnull EpicConfiguration epicConfiguration,
													 @Nullable Normalizer normalizer,
													 @Nullable String httpLoggingBaseName) {
		requireNonNull(epicConfiguration);

		this.epicConfiguration = epicConfiguration;
		this.httpClient = createHttpClient(epicConfiguration, httpLoggingBaseName);
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.normalizer = normalizer == null ? new Normalizer() : normalizer;
		this.dateFormatterHyphens = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US); // e.g. 1987-04-21
		this.dateFormatterSlashes = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US); // e.g. 6/8/2020
		this.amPmTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US); // e.g. "8:00 AM"
		this.militaryTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US); // e.g. "08:00"
		this.phoneNumberPattern = Pattern.compile("^\\d{3}-\\d{3}-\\d{4}$");
		this.logger = LoggerFactory.getLogger(getClass());
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

		return makeGetApiCall("api/FHIR/DSTU2/Patient",
				queryParameters,
				(responseBody) -> {
					return getGson().fromJson(responseBody, PatientSearchResponse.class);
				});
	}

	@Nonnull
	@Override
	public GetPatientDemographicsResponse performGetPatientDemographics(@Nonnull GetPatientDemographicsRequest request) {
		requireNonNull(request);

		return makeApiCall(HttpMethod.POST,
				"api/epic/2015/Common/Patient/GetPatientDemographics/Patient/Demographics",
				getGson().toJson(request),
				(responseBody) -> {
					return getGson().fromJson(responseBody, GetPatientDemographicsResponse.class);
				});
	}

	@Nonnull
	@Override
	public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
		requireNonNull(request);

		return makeApiCall(HttpMethod.POST,
				"api/epic/2012/Scheduling/Provider/GETPROVIDERSCHEDULE/Schedule",
				new HashMap<String, Object>() {{
					put("ProviderID", request.getProviderID());
					put("ProviderIDType", request.getProviderIDType());
					put("DepartmentID", request.getDepartmentID());
					put("DepartmentIDType", request.getDepartmentIDType());
					put("UserID", request.getUserID());
					put("UserIDType", request.getUserIDType());
					put("VisitTypeID", request.getVisitTypeID());
					put("VisitTypeIDType", request.getVisitTypeIDType());
					put("Date", formatDateWithSlashes(request.getDate()));
				}},
				null,
				(responseBody) -> {
					return getGson().fromJson(responseBody, GetProviderScheduleResponse.class);
				});
	}

	@Nonnull
	@Override
	public GetPatientAppointmentsResponse performGetPatientAppointments(@Nonnull GetPatientAppointmentsRequest request) {
		requireNonNull(request);

		return makeApiCall(HttpMethod.POST,
				"api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
				getGson().toJson(request),
				(responseBody) -> {
					return getGson().fromJson(responseBody, GetPatientAppointmentsResponse.class);
				});
	}

	@Nonnull
	@Override
	public PatientCreateResponse performPatientCreate(@Nonnull PatientCreateRequest request) {
		requireNonNull(request);

		return makeApiCall(HttpMethod.POST,
				"api/epic/2012/EMPI/External/PatientCreate/Patient/Create",
				getGson().toJson(request),
				(responseBody) -> {
					return getGson().fromJson(responseBody, PatientCreateResponse.class);
				});
	}

	@Nonnull
	@Override
	public ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(@Nonnull ScheduleAppointmentWithInsuranceRequest request) {
		requireNonNull(request);

		return makeApiCall(HttpMethod.POST,
				"api/epic/2014/PatientAccess/External/ScheduleAppointmentWithInsurance/Scheduling/Open/ScheduleWithInsurance",
				getGson().toJson(request),
				(responseBody) -> {
					return getGson().fromJson(responseBody, ScheduleAppointmentWithInsuranceResponse.class);
				});
	}

	@Nonnull
	@Override
	public CancelAppointmentResponse performCancelAppointment(@Nonnull CancelAppointmentRequest request) {
		requireNonNull(request);

		return makeApiCall(HttpMethod.POST,
				"api/epic/2014/PatientAccess/External/CancelAppointment/Scheduling/Cancel",
				getGson().toJson(request),
				(responseBody) -> {
					return getGson().fromJson(responseBody, CancelAppointmentResponse.class);
				});
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
	protected <T> T makeGetApiCall(@Nonnull String url,
																 @Nonnull Function<String, T> responseBodyMapper) {
		return makeGetApiCall(url, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeGetApiCall(@Nonnull String url,
																 @Nullable Map<String, Object> queryParameters,
																 @Nonnull Function<String, T> responseBodyMapper) {
		return makeApiCall(HttpMethod.GET, url, queryParameters, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String url,
															@Nullable String requestBody,
															@Nonnull Function<String, T> responseBodyMapper) {
		return makeApiCall(httpMethod, url, null, requestBody, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String url,
															@Nullable Map<String, Object> queryParameters,
															@Nonnull Function<String, T> responseBodyMapper) {
		return makeApiCall(httpMethod, url, queryParameters, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String url,
															@Nullable Map<String, Object> queryParameters,
															@Nullable String requestBody,
															@Nonnull Function<String, T> responseBodyMapper) {
		requireNonNull(httpMethod);
		requireNonNull(url);
		requireNonNull(responseBodyMapper);

		if (queryParameters == null)
			queryParameters = Collections.emptyMap();

		String finalUrl = format("%s/%s", getEpicConfiguration().getBaseUrl(), url);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		// Allow configuration to modify headers
		getEpicConfiguration().getRequestHeaderCustomizer().accept(headers);

		HttpRequest.Builder httpRequestBuilder = new HttpRequest.Builder(httpMethod, finalUrl)
				.headers(Collections.<String, Object>unmodifiableMap(headers));

		if (queryParameters.size() > 0)
			httpRequestBuilder.queryParameters(queryParameters);

		if (requestBody != null)
			httpRequestBuilder.body(requestBody);

		if (httpMethod == HttpMethod.POST)
			httpRequestBuilder.contentType("application/json; charset=utf-8");

		HttpRequest httpRequest = httpRequestBuilder.build();

		String queryParametersDescription = queryParameters.size() == 0 ? "[none]" : queryParameters.toString();
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
				return responseBodyMapper.apply(responseBody);
			} catch (Exception e) {
				throw new EpicException(format("Unable to parse JSON for EPIC endpoint %s %s with query params %s and request body %s. Response body was\n%s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody));
			}
		} catch (IOException e) {
			throw new EpicException(format("Unable to call EPIC endpoint %s %s with query params %s and request body %s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription), e);
		}
	}

	@Nonnull
	protected HttpClient createHttpClient(@Nonnull EpicConfiguration epicConfiguration,
																				@Nullable String httpLoggingBaseName) {
		requireNonNull(epicConfiguration);

		httpLoggingBaseName = httpLoggingBaseName == null ? "com.cobaltplatform.api.integration.epic" : httpLoggingBaseName;
		return new DefaultHttpClient(httpLoggingBaseName, epicConfiguration.getPermitUnsafeCerts());
	}

	@Nonnull
	protected String prettyPrintJson(@Nonnull String json) {
		Map<String, Object> map = getGson().fromJson(json, new TypeToken<Map<String, Object>>() {
		}.getType());
		return getGson().toJson(map);
	}

	@Nonnull
	@Override
	public EpicConfiguration getEpicConfiguration() {
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
}
