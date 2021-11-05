package com.cobaltplatform.ic.backend.model.response;

import com.cobaltplatform.ic.model.DispositionFlag;

public class FlagResponse {
    private int id;
    private String type;
    private String label;

    public FlagResponse(){};

    public FlagResponse(int id, String type, String label) {
        this.id = id;
        this.type = type;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public static FlagResponse forDispositionFlag(DispositionFlag dflag){
        return new FlagResponse(dflag.getId(), dflag.getType().toString(),
                dflag.getLabel());
    }
}
