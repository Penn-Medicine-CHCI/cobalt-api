package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.DReferralOrderReport;
import com.cobaltplatform.ic.backend.model.db.query.QDReferralOrderReport;
import com.cobaltplatform.ic.model.DispositionFlag;
import io.ebean.test.ForTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Transmogrify, LLC.
 */
public class OrderReportServiceTest {
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
	@Tag("requiresCobalt")
	void testCsvParsing() throws IOException {
		OrderReportService orderReportService = new OrderReportService();

		try (InputStream inputStream = getClass().getResourceAsStream("test-order-report.csv")) {
			List<DReferralOrderReport> orderReports = orderReportService.parseOrderReportCsv(inputStream);
			assertEquals("TEST-UID-1", orderReports.get(0).getUid());
		}
	}

	@Test
	@Tag("requiresCobalt")
	void processOrderRecordCreatesPatientAndDisposition() {
		DReferralOrderReport orderItem = new DReferralOrderReport()
				.setOrderId("1")
				.setFirstName("John")
				.setLastName("Doe")
				.setUid("12341324");

		DReferralOrderReport updated = OrderReportService.getSharedInstance().processOrderRecord(orderItem);
		DPatientDisposition disposition = updated.getDisposition();
		DPatient patient = disposition.getPatient();

		assertAll("Report",
				() -> assertNotNull(disposition),
				() -> assertNotNull(patient),
				() -> Assertions.assertEquals(disposition.getFlag(), DispositionFlag.NOT_YET_SCREENED),
				() -> assertEquals("John", patient.getPreferredFirstName()),
				() -> assertEquals("Doe", patient.getPreferredLastName())
		);
	}

	@Test
	@Tag("requiresCobalt")
	void processOrderRecordFindsAnExistingPatientByMRN() {
		DReferralOrderReport orderItem = new DReferralOrderReport()
				.setOrderId("1")
				.setFirstName("???")
				.setLastName("???")
				.setUid("12341324");

		DPatient p = new DPatient()
				.setPreferredFirstName("Jane")
				.setPreferredLastName("Doe")
				.setUid("12341324");
		p.save();

		DReferralOrderReport updated = OrderReportService.getSharedInstance().processOrderRecord(orderItem);
		DPatientDisposition disposition = updated.getDisposition();
		DPatient patient = disposition.getPatient();

		assertAll("Report",
				() -> assertNotNull(disposition),
				() -> assertNotNull(patient),
				() -> Assertions.assertEquals(disposition.getFlag(), DispositionFlag.NOT_YET_SCREENED),
				() -> assertEquals("Jane", patient.getPreferredFirstName()),
				() -> assertEquals("Doe", patient.getPreferredLastName())
		);
	}

	@Test
	@Tag("requiresCobalt")
	void processOrderRecordFindsAnExistingNonGraduatedDisposition() {
		DReferralOrderReport orderItem = new DReferralOrderReport()
				.setOrderId("1")
				.setFirstName("???")
				.setLastName("???")
				.setUid("12341324");

		DPatient p = new DPatient()
				.setPreferredFirstName("Jane")
				.setPreferredLastName("Doe")
				.setUid("12341324");
		p.save();

		DPatientDisposition d = new DPatientDisposition()
				.setPatient(p)
				.setFlag(DispositionFlag.IN_IC_TREATMENT);
		d.save();

		DReferralOrderReport updated = OrderReportService.getSharedInstance().processOrderRecord(orderItem);
		DPatientDisposition disposition = updated.getDisposition();
		DPatient patient = disposition.getPatient();

		assertAll("Report",
				() -> assertNotNull(disposition),
				() -> assertNotNull(patient),
				() -> Assertions.assertEquals(disposition.getFlag(), DispositionFlag.IN_IC_TREATMENT),
				() -> assertEquals("Jane", patient.getPreferredFirstName()),
				() -> assertEquals("Doe", patient.getPreferredLastName()),
				() -> assertEquals("12341324", patient.getUid())
		);
	}

	@Test
	@Tag("requiresCobalt")
	void processOrderRecordCreatesANewDispositionIfGraduated() {
		DReferralOrderReport orderItem = new DReferralOrderReport()
				.setOrderId("1")
				.setFirstName("???")
				.setLastName("???")
				.setUid("12341324");

		DPatient p = new DPatient()
				.setPreferredFirstName("Jane")
				.setPreferredLastName("Doe")
				.setUid("12341324");
		p.save();

		DPatientDisposition d = new DPatientDisposition()
				.setPatient(p)
				.setFlag(DispositionFlag.GRADUATED);
		d.save();

		DReferralOrderReport updated = OrderReportService.getSharedInstance().processOrderRecord(orderItem);
		DPatientDisposition disposition = updated.getDisposition();
		DPatient patient = disposition.getPatient();

		assertAll("Report",
				() -> assertNotNull(disposition),
				() -> assertNotNull(patient),
				() -> Assertions.assertEquals(disposition.getFlag(), DispositionFlag.NOT_YET_SCREENED),
				() -> assertEquals("Jane", patient.getPreferredFirstName()),
				() -> assertEquals("Doe", patient.getPreferredLastName()),
				() -> assertEquals("12341324", patient.getUid())
		);
	}


	@Test
	@Tag("requiresCobalt")
	void processOrderRecordDoesntProcessADuplicate() {
		DReferralOrderReport orderItem1 = new DReferralOrderReport()
				.setOrderId("1")
				.setFirstName("???")
				.setLastName("???")
				.setUid("12341324");

		DReferralOrderReport orderItem2 = new DReferralOrderReport()
				.setOrderId("1")
				.setFirstName("???")
				.setLastName("???")
				.setUid("12341324");

		OrderReportService.getSharedInstance().processOrderRecord(orderItem1);
		OrderReportService.getSharedInstance().processOrderRecord(orderItem2);

		Assertions.assertEquals(1, new QDReferralOrderReport().findCount());
	}

}
