package com.cobaltplatform.ic.model;

public enum LOINCAnswerCode {
    YES("LA33-6"),
    NEVER("LA6270-8"),
    PC_0("PC_0"),
    NOT_AT_ALL("LA6568-5");

    private final String answerId;

    LOINCAnswerCode(String answerId){
        this.answerId = answerId;
    }

    public String getAnswerId() {
        return answerId;
    }
}
