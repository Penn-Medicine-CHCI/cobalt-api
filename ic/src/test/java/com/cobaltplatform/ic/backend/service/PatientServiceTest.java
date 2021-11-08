package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.model.AssessmentStatus;
import com.cobaltplatform.ic.model.DispositionFlag;
import io.ebean.test.ForTests;
import io.javalin.http.NotFoundResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PatientServiceTest {
	private ForTests.RollbackAll rollbackAll;

	@BeforeEach
	public void before() {
		rollbackAll = ForTests.createRollbackAll();
	}

	@AfterEach
	public void after() {
		rollbackAll.close();
	}

	@Test
	void getOrCreate_ExistingUser_CreatesAssessment() {
		var patient = new DPatient();
		patient.save();

		var assessment = new PatientService().getOrCreateDispositionAndAssessmentForPatient(patient.getId()).get();
		Assertions.assertEquals(patient, assessment.getPatient());
		Assertions.assertEquals(patient, assessment.getDisposition().getPatient());
	}

	@Test
	void getOrCreate_ExistingUser_WithAssessment() {
		var patient = new DPatient();
		patient.save();

		var disposition = new DPatientDisposition().setPatient(patient);
		var assessment = new DAssessment().setPatient(patient).setDisposition(disposition);

		var assessmentRet = new PatientService().getOrCreateDispositionAndAssessmentForPatient(patient.getId()).get();
		Assertions.assertEquals(patient, assessmentRet.getPatient());
	}

	@Test
	void getOrCreate_ExistingUser_AlreadyAssessed() {
		var patient = new DPatient();
		patient.save();

		var disposition = new DPatientDisposition().setPatient(patient).setFlag(DispositionFlag.IN_IC_TREATMENT);
		disposition.save();
		var assessment = new DAssessment().setPatient(patient).setDisposition(disposition).setStatus(AssessmentStatus.COMPLETED);
		assessment.save();

		Assertions.assertEquals(assessment.getId(), new PatientService()
				.getOrCreateDispositionAndAssessmentForPatient(patient.getId()).orElseThrow().getId());
	}

	@Test
	void getOrCreate_ExistingUser_PatientNotFound() {
		try {
			new PatientService().getOrCreateDispositionAndAssessmentForPatient(UUID.randomUUID());
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NotFoundResponse);
		}
	}
}