package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.exception.CobaltException;
import com.cobaltplatform.ic.backend.model.cobalt.CreateOrderReportPatientRequest;
import com.cobaltplatform.ic.backend.model.cobalt.CreatePatientResponse;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.DReferralOrderReport;
import com.cobaltplatform.ic.backend.model.db.query.QDPatient;
import com.cobaltplatform.ic.backend.model.db.query.QDReferralOrderReport;
import io.ebean.annotation.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class OrderReportService {
	@Nonnull
	private static final OrderReportService SHARED_INSTANCE;

	@Nonnull
	private final CobaltService cobaltService;
	@Nonnull
	private final CSVFormat csvFormat;
	@Nonnull
	private final Logger logger;

	static {
		SHARED_INSTANCE = new OrderReportService();
	}

	public OrderReportService() {
		this(CobaltService.getSharedInstance());
	}

	public OrderReportService(@Nonnull CobaltService cobaltService) {
		requireNonNull(cobaltService);

		this.cobaltService = cobaltService;
		this.csvFormat = CSVFormat.EXCEL.withFirstRecordAsHeader();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Transactional
	public DReferralOrderReport processOrderRecord(DReferralOrderReport item){
		if(new QDReferralOrderReport()
				.orderId.equalTo(item.getOrderId())
				.exists()) {
			getLogger().warn("Duplicate Order Report {}", item.getOrderId());
			return item;
		}

		var isDigital = item.getCcbhOrderRouting() != null && item.getCcbhOrderRouting().toLowerCase(Locale.ROOT).contains("digital");

		DPatient patient = new QDPatient()
				.uid.equalTo(item.getUid()).findOneOrEmpty()
				.orElseGet(() -> {
					CreateOrderReportPatientRequest request = new CreateOrderReportPatientRequest(item.getUid(), item.getFirstName(), item.getLastName());
					CreatePatientResponse response;

					try {
						response = getCobaltService().createPatient(request);
					} catch(CobaltException e) {
						String errorMessage = format("Unable to successfully create Cobalt patient with MRN '%s' and UID '%s' from order report", item.getMrn(), item.getUid());
						getLogger().error(errorMessage, e);
						throw new RuntimeException(errorMessage, e);
					}

					DPatient newPatient = new DPatient()
							.setCobaltAccountId(response.getAccount().getAccountId())
							.setUid(item.getUid())
							.setPreferredFirstName(item.getFirstName())
							.setPreferredLastName(item.getLastName())
							.setPreferredGender(item.getSex())
							.setPreferredPhoneNumber(item.getCallBackNumber());
					newPatient.save();
					return newPatient;
		});

		DPatientDisposition disposition = DispositionService.getLatestDispositionForPatient(patient.getId())
				.orElseGet(() -> DispositionService.createDisposition(patient, isDigital));

		item.setDisposition(disposition);
		disposition.addOrder(item);
		item.save();
		disposition.save();

		return item;
	}

	@Nonnull
	public List<DReferralOrderReport> parseOrderReportCsv(@Nonnull InputStream inputStream) {
		requireNonNull(inputStream);

		BOMInputStream bOMInputStream = new BOMInputStream(inputStream);
		ByteOrderMark bom = null;
		try {
			bom = bOMInputStream.getBOM();
		} catch (IOException e) {
			logger.error("Error reading BOM", e);
			throw new UncheckedIOException(e);
		}

		String charsetName = bom == null ? "UTF-8" : bom.getCharsetName();

		List<DReferralOrderReport> orderReports = new ArrayList<>();

		try (Reader csvReader = new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName)) {
			for (CSVRecord csvRecord : getCsvFormat().parse(csvReader)) {
				DReferralOrderReport orderReport = new DReferralOrderReport();
				orderReport.setEncounterDeptName(trimToNull(csvRecord.get("Encounter Dept Name")));
				orderReport.setEncounterDeptId(trimToNull(csvRecord.get("Encounter Dept ID")));
				orderReport.setReferringPractice(trimToNull(csvRecord.get(2))); // header is duplicate "Referring Practice" so we use index
				orderReport.setReferringPracticeSecond(trimToNull(csvRecord.get(3))); // header is duplicate "Referring Practice" so we use index
				orderReport.setOrderingProvider(trimToNull(csvRecord.get("Ordering Provider")));
				orderReport.setBillingProvider(trimToNull(csvRecord.get("Billing Provider")));
				orderReport.setLastName(trimToNull(csvRecord.get("Last Name")));
				orderReport.setFirstName(trimToNull(csvRecord.get("First Name")));
				orderReport.setMrn(trimToNull(csvRecord.get("MRN")));
				orderReport.setUid(trimToNull(csvRecord.get("UID")));
				orderReport.setSex(trimToNull(csvRecord.get("Sex")));
				orderReport.setDateOfBirth(trimToNull(csvRecord.get("DOB")));
				orderReport.setPrimaryPayor(trimToNull(csvRecord.get("Primary Payor")));
				orderReport.setPrimaryPlan(trimToNull(csvRecord.get("Primary Plan")));
				orderReport.setOrderDate(trimToNull(csvRecord.get("Order Date")));
				orderReport.setOrderId(trimToNull(csvRecord.get("Order ID")));
				orderReport.setAgeOfOrder(trimToNull(csvRecord.get("Age of Order")));
				orderReport.setCcbhOrderRouting(trimToNull(csvRecord.get("CCBH Order Routing")));
				orderReport.setReasonsForReferral(trimToNull(csvRecord.get("Reasons for Referral")));
				orderReport.setDx(trimToNull(csvRecord.get("DX")));
				orderReport.setOrderAssociatedDiagnosis(trimToNull(csvRecord.get("Order Associated Diagnosis (ICD-10)")));
				orderReport.setCallBackNumber(trimToNull(csvRecord.get("Call Back Number")));
				orderReport.setPreferredContactHours(trimToNull(csvRecord.get("Preferred Contact Hours")));
				orderReport.setOrderComments(trimToNull(csvRecord.get("Order Comments")));
				orderReport.setImgCcRecipients(trimToNull(csvRecord.get("IMG CC Recipients")));
				orderReport.setPatientAddressLine1(trimToNull(csvRecord.get("Patient Address (Line 1)")));
				orderReport.setPatientAddressLine2(trimToNull(csvRecord.get("Patient Address (Line 2)")));
				orderReport.setCity(trimToNull(csvRecord.get("City")));
				orderReport.setPatientState(trimToNull(csvRecord.get("Patient State")));
				orderReport.setPatientZipCode(trimToNull(csvRecord.get("ZIP Code")));
				orderReport.setCcbhLastActiveMedOrderSummary(trimToNull(csvRecord.get("CCBH Last Active Med Order Summary")));
				orderReport.setCcbhMedicationsList(trimToNull(csvRecord.get("CCBH Medications List")));
				orderReport.setPsychotherapeuticMedLstTwoWeeks(trimToNull(csvRecord.get("Psychotherapeutic Med Lst 2 Weeks")));

				orderReports.add(orderReport);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to parse order report CSV", e);
		}

		return orderReports;
	}

	public void persistOrderReports(List<DReferralOrderReport> orderReports) {
		orderReports.forEach(this::processOrderRecord);
	}

	@Nonnull
	public static OrderReportService getSharedInstance() {
		return SHARED_INSTANCE;
	}

	@Nonnull
	protected CobaltService getCobaltService() {
		return cobaltService;
	}

	@Nonnull
	protected CSVFormat getCsvFormat() {
		return csvFormat;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
