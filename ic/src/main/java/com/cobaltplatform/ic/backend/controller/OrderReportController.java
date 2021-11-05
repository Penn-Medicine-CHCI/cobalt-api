package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.model.db.DReferralOrderReport;
import com.cobaltplatform.ic.backend.service.OrderReportService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

@ThreadSafe
public class OrderReportController {
	@Nonnull
	public static Handler uploadOrderReportCsv = ctx -> {
		List<UploadedFile> uploadedFiles = ctx.uploadedFiles("patients");

		if (uploadedFiles == null || uploadedFiles.size() != 1)
			throw new BadRequestResponse("You must provide exactly one order report file.");

		UploadedFile uploadedFile = uploadedFiles.get(0);
		List<DReferralOrderReport> orderReports;

		try (InputStream inputStream = uploadedFile.getContent()) {
			orderReports = OrderReportService.getSharedInstance().parseOrderReportCsv(inputStream);
			OrderReportService.getSharedInstance().persistOrderReports(orderReports);
		}

		ctx.json(new HashMap<String, Object>() {{
			put("recordsParsed", orderReports.size());
		}});
	};
}
