package com.shanalert.hospitalalert.service;


import com.shanalert.hospitalalert.repository.HospitalAdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HospitalAdminService {

    @Autowired
    private HospitalAdminRepository hospitalAdminRepository;
}
