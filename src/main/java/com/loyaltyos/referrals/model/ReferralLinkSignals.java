package com.loyaltyos.referrals.model;

/**
 * Optional identity signals from integrator at link time (hashed for comparison only).
 */
public class ReferralLinkSignals {

    private String phone;
    private String email;
    private String deviceId;

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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
