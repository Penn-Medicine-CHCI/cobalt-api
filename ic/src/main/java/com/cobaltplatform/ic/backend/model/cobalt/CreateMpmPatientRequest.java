package com.cobaltplatform.ic.backend.model.cobalt;

import org.hl7.fhir.dstu3.model.Patient;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CreateMpmPatientRequest {
	@Nonnull
	private final Patient patient;

	public CreateMpmPatientRequest(@Nonnull Patient patient) {
		requireNonNull(patient);
		this.patient = patient;
	}

	@Nonnull
	public Patient getPatient() {
		return patient;
	}
}