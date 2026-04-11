package com.shanalert.hospitalalert.model;

public enum EmergencyType {
    CARDIAC_ARREST(BedType.ICU),
    SEVERE_TRAUMA(BedType.ICU),
    MATERNITY_EMERGENCY(BedType.MATERNITY),
    RESPIRATORY_DISTRESS(BedType.ICU),
    MINOR_INJURY(BedType.GENERAL_WARD);

    private final BedType requiredBedType;

    EmergencyType(BedType bedType) {
        this.requiredBedType = bedType;
    }

    public BedType getRequiredBedType() {
        return requiredBedType;
    }
}