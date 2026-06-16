package com.shanalert.hospitalalert.model;

import lombok.Getter;

@Getter
public enum HospusAPP {
    ADMIN_APP("admin_app"),
    HOSPITAL_APP("hospital_app"),
    PATIENT_APP("patient_app");

    private final String value;

    HospusAPP(String value) {
        this.value = value;
    }

    /**
     * Finds the enum based on the string value (case-insensitive)
     */
    public static HospusAPP fromString(String text) {
        for (HospusAPP app : HospusAPP.values()) {
            if (app.value.equalsIgnoreCase(text) || app.name().equalsIgnoreCase(text)) {
                return app;
            }
        }
        throw new IllegalArgumentException("Invalid App Source: " + text);
    }
}