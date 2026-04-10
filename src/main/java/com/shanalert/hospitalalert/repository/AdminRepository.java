package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
}
