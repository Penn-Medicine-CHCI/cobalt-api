package com.cobaltplatform.ic.backend.model.response;

import com.cobaltplatform.ic.backend.model.db.DReferralOrderReport;

public class OrderResponse {
    String mrn;
    String uid;
    String reasonForReferral;
    String dx;
    String billingProvider;
    String orderingProvider;
    String medications;
    String orderDate;
    String engagement;
    String insurance;

    public OrderResponse(){};

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getUid() { return uid; }

    public void setUid(String uid) { this.uid = uid; }

    public String getReasonForReferral() {
        return reasonForReferral;
    }

    public void setReasonForReferral(String reasonForReferral) {
        this.reasonForReferral = reasonForReferral;
    }

    public String getDx() {
        return dx;
    }

    public void setDx(String dx) {
        this.dx = dx;
    }

    public String getBillingProvider() {
        return billingProvider;
    }

    public void setBillingProvider(String billingProvider) {
        this.billingProvider = billingProvider;
    }

    public String getOrderingProvider() {
        return orderingProvider;
    }

    public void setOrderingProvider(String orderingProvider) {
        this.orderingProvider = orderingProvider;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getInsurance() {
        return insurance;
    }

    public void setInsurance(String insurance) {
        this.insurance = insurance;
    }

    public String getEngagement() {
        return engagement;
    }

    public void setEngagement(String engagement) {
        this.engagement = engagement;
    }

    public static OrderResponse forDispositionOrders(DReferralOrderReport order) {
        var orderResponse = new OrderResponse() ;
        orderResponse
                .setMedications(order.getCcbhMedicationsList());
        orderResponse
                .setDx(order.getDx());
        orderResponse
                .setOrderDate(order.getOrderDate());
        orderResponse
                .setReasonForReferral(order.getReasonsForReferral());
        orderResponse
                .setMrn(order.getMrn());
        orderResponse
                .setBillingProvider(order.getBillingProvider());
        orderResponse
                .setOrderingProvider(order.getOrderingProvider());
        orderResponse
                .setInsurance(order.getPrimaryPlan());
        orderResponse
                .setEngagement(order.getCcbhOrderRouting());
        orderResponse
                .setUid(order.getUid());

        return orderResponse;
    }

}
