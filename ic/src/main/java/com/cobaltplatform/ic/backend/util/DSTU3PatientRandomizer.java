package com.cobaltplatform.ic.backend.util;

import com.cobaltplatform.ic.backend.service.PatientService;
import org.hl7.fhir.dstu3.model.Patient;
import org.jeasy.random.api.Randomizer;

public class DSTU3PatientRandomizer implements Randomizer<Patient> {
    private PatientService service = new PatientService();
    @Override
    public Patient getRandomValue() {
        return service.getPatientObject("jane");
    }
}
