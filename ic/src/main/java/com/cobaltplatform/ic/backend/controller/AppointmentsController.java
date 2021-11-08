package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.service.AppointmentsService;
import io.javalin.http.Handler;

import java.util.Map;

public class AppointmentsController {
    private static final AppointmentsService appointmentsService = new AppointmentsService();

    public static Handler getPatientAppointments = ctx -> {
        String patientId = ctx.queryParam("patientId");
        Map[] appointments = appointmentsService.getAppointments(patientId);
        ctx.json(appointments);
    };
}
