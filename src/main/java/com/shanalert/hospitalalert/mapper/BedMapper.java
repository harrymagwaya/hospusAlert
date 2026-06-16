package com.shanalert.hospitalalert.mapper;

import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;

import com.shanalert.hospitalalert.entity.Bed;
import org.springframework.stereotype.Component;

@Component
public class BedMapper {


    public BedResponse toDto(Bed bed, String message) {
        if (bed == null) return null;

        return new BedResponse(
                bed.getId(),
                bed.getBedNumber(),
                bed.getBedType(),
                bed.getStatus(),
                message
        );
    }


    public void toEntity(BedUpdateRequest dto, Bed existingBed) {
        if (dto == null || existingBed == null) return;

//        if (dto.bedNumber() != null) {
//            existingBed.setBedNumber(dto.bedNumber());
//        }

        if (dto.bedType() != null) {
            existingBed.setBedType(dto.bedType());
        }

        if (dto.status() != null) {
            existingBed.setStatus(dto.status());
        }
    }
}