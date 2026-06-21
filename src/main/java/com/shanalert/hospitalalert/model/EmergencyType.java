package com.shanalert.hospitalalert.model;

public enum EmergencyType {
    CARDIAC_ARREST(BedType.ICU),
    SEVERE_TRAUMA(BedType.ICU),
    RESPIRATORY_DISTRESS(BedType.ICU),

    MATERNITY_EMERGENCY(BedType.MATERNITY),
    LABOUR_COMPLICATIONS(BedType.MATERNITY),
    NEONATAL_EMERGENCY(BedType.PEDIATRIC),

    STROKE(BedType.ICU),
    BURNS(BedType.ICU),
    POISONING(BedType.EMERGENCY),
    SEVERE_BLEEDING(BedType.EMERGENCY),
    ROAD_TRAFFIC_ACCIDENT(BedType.EMERGENCY),

    FRACTURE(BedType.SURGERY),
    APPENDICITIS(BedType.SURGERY),

    CHILD_EMERGENCY(BedType.PEDIATRIC),

    MINOR_INJURY(BedType.GENERAL_WARD),
    FEVER(BedType.GENERAL_WARD),
    MALARIA_SEVERE(BedType.GENERAL_WARD);

    private final BedType requiredBedType;

    EmergencyType(BedType bedType) {
        this.requiredBedType = bedType;
    }

    public BedType getRequiredBedType() {
        return requiredBedType;
    }
}