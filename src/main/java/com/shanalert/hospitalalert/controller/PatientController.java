package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.PatientUpdateDTO;
import com.shanalert.hospitalalert.entity.Patient;
import com.shanalert.hospitalalert.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;


    @GetMapping("/{id}")
    public Patient getPatient(@PathVariable UUID id) {
        // In the future, you can extract the ID from the JWT token directly
        // to make a true "/me" endpoint without requiring the ID in the URL.
        return patientService.getPatientProfile(id);
    }

    // 2. PATCH: Update medical history
    @PatchMapping("/{id}")
    public Patient patchPatientProfile(
            @PathVariable UUID id,
            @RequestBody PatientUpdateDTO dto) {
        return patientService.updatePatientMedicalData(id, dto);
    }


    // ... inside PatientController.java ...

    // 3. READ ALL: Get all patients (Paginated)
    @GetMapping
    public Page<Patient> getAll(
            @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
        return patientService.getAllPatients(pageable);
    }

    // 4. DELETE: Soft delete the profile
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        patientService.deletePatientProfile(id);
    }
}
