package com.cobaltplatform.ic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.ebean.annotation.DbEnumValue;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DispositionOutcomeCare {
    SUB_CLINICAL(-1),
    SELF_DIRECTED(0),
    IC(1),
    SPECIALTY(2);

    private long id;
    private String label;

    DispositionOutcomeCare(final long code) {
        this.id = code;
        this.label = this.name();
    }

    public String getLabel() {
        return label;
    }

    public DispositionOutcomeCare setLabel(final String label) {
        this.label = label;
        return this;
    }

    @DbEnumValue
    public long getId() {
        return id;
    }

    public void setId(long value) {
        this.id = value;
    }

    @JsonCreator
    public static DispositionOutcomeCare fromNode(JsonNode node) {
        if (!node.has("id"))
            return null;

        String name = node.get("label").asText();

        return DispositionOutcomeCare.valueOf(name);
    }

}
