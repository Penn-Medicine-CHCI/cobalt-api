/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.integration.epic.MockEpicClient;
import com.cobaltplatform.api.integration.epic.request.AddFlowsheetValueRequest;
import com.cobaltplatform.api.integration.epic.response.AddFlowsheetValueResponse;
import com.cobaltplatform.api.model.db.Flowsheet;
import com.cobaltplatform.api.model.db.FlowsheetType.FlowsheetTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.RawPatientOrder;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningAnswerResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningQuestionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningSessionScreeningResult;
import com.cobaltplatform.api.util.ValidationException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

@ThreadSafe
@Category(UnitTest.class)
public class EaseFlowsheetWritebackTests {
	@Nonnull
	private static final Instant COMPLETED_AT = Instant.parse("2026-09-17T14:00:00Z");
	@Nonnull
	private static final List<String> PHQ_FLOWSHEET_IDS = List.of(
			"3375", "3376", "3377", "5777", "3379", "3380", "3381", "3382", "3383");
	@Nonnull
	private static final List<String> PROMIS_FLOWSHEET_IDS = List.of(
			"1021500376", "1021500377", "1021500378", "1021500379",
			"1021500380", "1021500381", "1021500382", "1021500383");

	@Test
	public void writesAllAnsweredPhqAndPromisItemRowsWithEpicValues() {
		Map<UUID, Integer> displayOrdersByQuestionId = new HashMap<>();
		ScreeningSessionResult result = screeningSessionResult(
				List.of(2, 2, 1, 0, 1, 0, 1, 0, 0),
				List.of(1, 2, 3, 4, 5, 1, 2, 3),
				displayOrdersByQuestionId);
		CapturingEpicClient epicClient = new CapturingEpicClient();

		List<AddFlowsheetValueRequest> requests = EaseFlowsheetWriteback.writeFlowsheetValues(
				result,
				displayOrdersByQuestionId::get,
				flowsheets(),
				patientOrder(),
				institution(),
				"123456789",
				COMPLETED_AT,
				epicClient);

		assertEquals(17, requests.size());
		assertEquals(requests, epicClient.getRequests());
		Map<String, AddFlowsheetValueRequest> requestsByFlowsheetId = requests.stream()
				.collect(Collectors.toMap(AddFlowsheetValueRequest::getFlowsheetID, Function.identity()));

		assertEquals("2", requestsByFlowsheetId.get("3375").getValue());
		assertEquals("2", requestsByFlowsheetId.get("3376").getValue());
		assertEquals("1 - Several days", requestsByFlowsheetId.get("3377").getValue());
		assertEquals("0 - Not at all", requestsByFlowsheetId.get("5777").getValue());
		assertEquals("1", requestsByFlowsheetId.get("1021500376").getValue());
		assertEquals("5", requestsByFlowsheetId.get("1021500380").getValue());
		assertEquals("310", requestsByFlowsheetId.get("3375").getFlowsheetTemplateID());
		assertEquals("375", requestsByFlowsheetId.get("1021500376").getFlowsheetTemplateID());

		for (AddFlowsheetValueRequest request : requests) {
			assertEquals("patient-uid", request.getPatientID());
			assertEquals("UID", request.getPatientIDType());
			assertEquals("123456789", request.getContactID());
			assertEquals("CSN", request.getContactIDType());
			assertEquals("INTERNAL", request.getFlowsheetIDType());
			assertEquals("INTERNAL", request.getFlowsheetTemplateIDType());
			assertEquals("COBALT", request.getUserID());
			assertEquals("EXTERNAL", request.getUserIDType());
			assertEquals(COMPLETED_AT, request.getInstantValueToken());
			assertNull(request.getComment());
		}
	}

	@Test
	public void writesOnlyAnsweredPhqRowsWhenPhqTwoCompletesEarly() {
		Map<UUID, Integer> displayOrdersByQuestionId = new HashMap<>();
		ScreeningSessionResult result = screeningSessionResult(
				List.of(0, 1),
				List.of(5, 4, 3, 2, 1, 5, 4, 3),
				displayOrdersByQuestionId);
		CapturingEpicClient epicClient = new CapturingEpicClient();

		EaseFlowsheetWriteback.writeFlowsheetValues(result, displayOrdersByQuestionId::get, flowsheets(),
				patientOrder(), institution(), "123456789", COMPLETED_AT, epicClient);

		assertEquals(10, epicClient.getRequests().size());
		assertEquals(List.of("3375", "3376"), epicClient.getRequests().stream()
				.map(AddFlowsheetValueRequest::getFlowsheetID)
				.filter(PHQ_FLOWSHEET_IDS::contains)
				.toList());
	}

	@Test
	public void validatesAllConfigurationBeforeWritingAnyRows() {
		Map<UUID, Integer> displayOrdersByQuestionId = new HashMap<>();
		ScreeningSessionResult result = screeningSessionResult(List.of(0, 1), List.of(), displayOrdersByQuestionId);
		CapturingEpicClient epicClient = new CapturingEpicClient();
		List<Flowsheet> flowsheets = flowsheets().stream()
				.filter(flowsheet -> flowsheet.getFlowsheetTypeId() != FlowsheetTypeId.PHQ9_QUESTION_2)
				.toList();

		assertThrows(ValidationException.class, () -> EaseFlowsheetWriteback.writeFlowsheetValues(
				result, displayOrdersByQuestionId::get, flowsheets, patientOrder(), institution(),
				"123456789", COMPLETED_AT, epicClient));
		assertEquals(0, epicClient.getRequests().size());
	}

	@Test
	public void selectsNewestCompletedClinicalSession() {
		ScreeningSession incomplete = screeningSession(false, Instant.parse("2026-09-17T16:00:00Z"));
		ScreeningSession older = screeningSession(true, Instant.parse("2026-09-17T13:00:00Z"));
		ScreeningSession newest = screeningSession(true, Instant.parse("2026-09-17T15:00:00Z"));

		assertSame(newest, EaseFlowsheetWriteback.findNewestCompletedScreeningSession(
				List.of(older, incomplete, newest)));
	}

	@Nonnull
	private ScreeningSessionResult screeningSessionResult(@Nonnull List<Integer> phqScores,
																								 @Nonnull List<Integer> promisScores,
																								 @Nonnull Map<UUID, Integer> displayOrdersByQuestionId) {
		ScreeningSessionResult result = new ScreeningSessionResult();
		result.setScreeningSessionScreeningResults(List.of(
				screeningResult(ScreeningTypeId.PHQ_9, phqScores, displayOrdersByQuestionId),
				screeningResult(ScreeningTypeId.PROMIS_PARTICIPATION_SOCIAL_ROLES_8A_V1,
						promisScores, displayOrdersByQuestionId)));
		return result;
	}

	@Nonnull
	private ScreeningSessionScreeningResult screeningResult(@Nonnull ScreeningTypeId screeningTypeId,
																											 @Nonnull List<Integer> scores,
																											 @Nonnull Map<UUID, Integer> displayOrdersByQuestionId) {
		List<ScreeningQuestionResult> questionResults = new ArrayList<>();

		for (int index = 0; index < scores.size(); ++index) {
			UUID screeningQuestionId = UUID.randomUUID();
			displayOrdersByQuestionId.put(screeningQuestionId, index + 1);

			ScreeningAnswerResult answerResult = new ScreeningAnswerResult();
			answerResult.setScore(scores.get(index));

			ScreeningQuestionResult questionResult = new ScreeningQuestionResult();
			questionResult.setScreeningQuestionId(screeningQuestionId);
			questionResult.setScreeningAnswerResults(List.of(answerResult));
			questionResults.add(questionResult);
		}

		ScreeningSessionScreeningResult result = new ScreeningSessionScreeningResult();
		result.setScreeningTypeId(screeningTypeId);
		result.setScreeningQuestionResults(questionResults);
		return result;
	}

	@Nonnull
	private List<Flowsheet> flowsheets() {
		List<Flowsheet> flowsheets = new ArrayList<>();
		List<FlowsheetTypeId> phqTypes = List.of(
				FlowsheetTypeId.PHQ9_QUESTION_1, FlowsheetTypeId.PHQ9_QUESTION_2,
				FlowsheetTypeId.PHQ9_QUESTION_3, FlowsheetTypeId.PHQ9_QUESTION_4,
				FlowsheetTypeId.PHQ9_QUESTION_5, FlowsheetTypeId.PHQ9_QUESTION_6,
				FlowsheetTypeId.PHQ9_QUESTION_7, FlowsheetTypeId.PHQ9_QUESTION_8,
				FlowsheetTypeId.PHQ9_QUESTION_9);
		List<FlowsheetTypeId> promisTypes = List.of(
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_1,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_2,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_3,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_4,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_5,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_6,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_7,
				FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_8);

		for (int index = 0; index < phqTypes.size(); ++index)
			flowsheets.add(flowsheet(phqTypes.get(index), PHQ_FLOWSHEET_IDS.get(index), "310"));

		for (int index = 0; index < promisTypes.size(); ++index)
			flowsheets.add(flowsheet(promisTypes.get(index), PROMIS_FLOWSHEET_IDS.get(index), "375"));

		return flowsheets;
	}

	@Nonnull
	private Flowsheet flowsheet(@Nonnull FlowsheetTypeId flowsheetTypeId,
														 @Nonnull String epicFlowsheetId,
														 @Nonnull String epicFlowsheetTemplateId) {
		Flowsheet flowsheet = new Flowsheet();
		flowsheet.setFlowsheetTypeId(flowsheetTypeId);
		flowsheet.setInstitutionId(InstitutionId.COBALT_IC_EASE);
		flowsheet.setEpicFlowsheetId(epicFlowsheetId);
		flowsheet.setEpicFlowsheetIdType("INTERNAL");
		flowsheet.setEpicFlowsheetTemplateId(epicFlowsheetTemplateId);
		flowsheet.setEpicFlowsheetTemplateIdType("INTERNAL");
		return flowsheet;
	}

	@Nonnull
	private RawPatientOrder patientOrder() {
		RawPatientOrder patientOrder = new RawPatientOrder();
		patientOrder.setInstitutionId(InstitutionId.COBALT_IC_EASE);
		patientOrder.setPatientUniqueId("patient-uid");
		patientOrder.setPatientUniqueIdType("UID");
		return patientOrder;
	}

	@Nonnull
	private Institution institution() {
		Institution institution = new Institution();
		institution.setInstitutionId(InstitutionId.COBALT_IC_EASE);
		institution.setEpicUserId("COBALT");
		institution.setEpicUserIdType("EXTERNAL");
		return institution;
	}

	@Nonnull
	private ScreeningSession screeningSession(boolean completed, @Nonnull Instant completedAt) {
		ScreeningSession screeningSession = new ScreeningSession();
		screeningSession.setCompleted(completed);
		screeningSession.setCompletedAt(completedAt);
		return screeningSession;
	}

	private static class CapturingEpicClient extends MockEpicClient {
		@Nonnull
		private final List<AddFlowsheetValueRequest> requests = new ArrayList<>();

		@Nonnull
		@Override
		public AddFlowsheetValueResponse addFlowsheetValue(@Nonnull AddFlowsheetValueRequest request) {
			this.requests.add(request);
			return new AddFlowsheetValueResponse();
		}

		@Nonnull
		public List<AddFlowsheetValueRequest> getRequests() {
			return List.copyOf(this.requests);
		}
	}
}
