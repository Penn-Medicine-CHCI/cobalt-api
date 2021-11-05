package com.cobaltplatform.ic.backend.model.db;

import org.hl7.fhir.r4.model.Coding;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Optional;

@Entity
@Table(name = "response_item", schema = "ic")
public class DResponseItem extends DBaseModel {
    public DResponseItem(DAssessment assessment, String linkId){
        this.assessment = assessment;
        this.linkId = linkId;
    }

    @ManyToOne
    private final DAssessment assessment;
    private final String linkId;

    private String stringValue;
    private Boolean booleanValue;
    private String codeSystem;
    private String codeValue;

    public String getLinkId(){
        return this.linkId;
    }

    public Optional<Coding> getCodingValue(){
        if(this.codeValue != null){
            return Optional.of(new Coding(codeSystem, codeValue, stringValue));
        }
        return Optional.empty();
    }

    public DResponseItem setCodingValue(Coding coding){
        this.stringValue = coding.getDisplay();
        this.booleanValue = null;
        this.codeSystem = coding.getSystem();
        this.codeValue = coding.getCode();
        return this;
    }

    public DResponseItem setBooleanValue(Boolean booleanValue){
        this.stringValue = null;
        this.booleanValue = booleanValue;
        this.codeSystem = null;
        this.codeValue = null;
        return this;
    }

    public Optional<Boolean> getBooleanValue(){
        return Optional.ofNullable(booleanValue);
    }

    public DResponseItem setStringValue(String stringValue){
        this.stringValue = stringValue;
        this.booleanValue = null;
        this.codeSystem = null;
        this.codeValue = null;
        return this;
    }

    public Optional<String> getStringValue(){
        if(this.codeValue == null){
            return Optional.ofNullable(stringValue);
        }
        return Optional.empty();
    }
}
