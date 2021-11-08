package com.cobaltplatform.ic.backend.model.serialize;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;

public class FhirR4Serializer extends StdSerializer<IBaseResource> {
    private final FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);

    public FhirR4Serializer(final Class<IBaseResource> t) {
        super(t);
    }
    public FhirR4Serializer() {
        this(null);
    }

    @Override
    public void serialize(final IBaseResource value, final JsonGenerator gen, final SerializerProvider provider)
            throws IOException {
        IParser writer = context.newJsonParser();
        var serializedPatient = writer.encodeResourceToString(value);
        gen.writeRawValue(serializedPatient);
    }
}
