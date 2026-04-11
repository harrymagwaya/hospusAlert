package com.shanalert.hospitalalert.model;

public enum AlertStatus {
    PENDING,       // Alert triggered, bed reservation in progress
    ON_THE_WAY,    // Bed reserved, patient is in transit
    ARRIVED,       // Patient is at the hospital facility (Gate/Reception)
    OCCUPIED,      // Patient is in the bed (The "Checked-in" state)
    COMPLETED,     // Treatment finished, bed released
    CANCELLED,     // Request aborted
    FAILED

}
