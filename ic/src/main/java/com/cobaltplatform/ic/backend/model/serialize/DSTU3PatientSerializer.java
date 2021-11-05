package com.cobaltplatform.ic.backend.model.serialize;

import java.io.IOException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.hl7.fhir.dstu3.model.Patient;

public class DSTU3PatientSerializer extends StdSerializer<Patient> {
    private final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);

    public DSTU3PatientSerializer(final Class<Patient> t) {
        super(t);
    }
    public DSTU3PatientSerializer() {
        this(null);
    }
    @Override
    public void serialize(final Patient value, final JsonGenerator gen, final SerializerProvider provider)
        throws IOException {
        var serializedPatient = fhirContext.newJsonParser().encodeResourceToString(value);
        gen.writeRawValue(serializedPatient);
    }
}
