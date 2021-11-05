package com.cobaltplatform.ic.backend.model.serialize;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReviewedBy {
    private String mhic;

    @JsonProperty("mhic")
    public String getMhic() {
        return mhic;
    }

    @JsonProperty("mhic")
    public void setMhic(String value) {
        this.mhic = value;
    }
}

