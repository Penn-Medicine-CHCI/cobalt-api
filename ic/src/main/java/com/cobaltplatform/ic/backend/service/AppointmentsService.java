package com.cobaltplatform.ic.backend.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppointmentsService {
    public Map[] getAppointments(String patientId) {
        try {
            File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("jane-appointments" + ".json"))
                    .getFile());
            ObjectMapper mapper = new ObjectMapper();
            Map[] map = mapper.readValue(file, Map[].class);
            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
