package com.cobaltplatform.ic.backend.model.response;

public class PatientDemographicsDTO {
    String phone;
    String email;

    public PatientDemographicsDTO(){};

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
