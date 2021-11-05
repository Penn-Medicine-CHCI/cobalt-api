package com.cobaltplatform.ic.backend.model.serialize;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.io.StringReader;

public class FhirR4Deserializer<T extends IBaseResource> extends StdDeserializer<T> {
    private final Class<T> deserializerClass;

    private final FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);
    public FhirR4Deserializer(Class<T> vc) {
        super(vc);
        this.deserializerClass = vc;
    }

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        IParser parser = context.newJsonParser();

        TreeNode tree = jsonParser.getCodec().readTree(jsonParser);
        StringReader reader = new StringReader(tree.toString());
        return parser.parseResource(deserializerClass, reader);
    }
}
