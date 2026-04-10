package com.shanalert.hospitalalert.model;

public enum DoctorStatus {
    ON_DUTY_AVAILABLE,
    ON_DUTY_BUSY,     // Currently with another patient
    IN_SURGERY,       // Do not disturb
    OFF_DUTY,
    ON_LEAVE,
    EMERGENCY_ONLY
}