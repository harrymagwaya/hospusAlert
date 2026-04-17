package com.shanalert.hospitalalert.controller;


import com.shanalert.hospitalalert.dto.UserRequest;
import com.shanalert.hospitalalert.dto.UserResponse;
import com.shanalert.hospitalalert.dto.UserUpdateDTO;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.model.UserStatus;
import com.shanalert.hospitalalert.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Page<UserResponse> getAllUsers(
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return userService.getAllUsers(status, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    // RESTRICTED: Both System Admin and Hospital Admin can onboard staff
    @PostMapping("/staff/onboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse onboardStaff(@Valid @RequestBody UserRequest request) {

        // Safety Logic: A Hospital Admin should not be able to create another Super Admin
        if (request.getUserRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Super Admins can create other Admins.");
        }

        return userService.registerUser(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerPatient(@Valid @RequestBody UserRequest request) {
        request.setUserRole(UserRole.PATIENT); // Force role to Patient
        return userService.registerUser(request);
    }

    @PatchMapping("/{id}")
    public UserResponse patchUser(
            @PathVariable UUID id,
            @RequestBody UserUpdateDTO updateDto) {
        return userService.updateUser(id, updateDto);
    }

    @DeleteMapping("/{id}")
    public void softDeleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}
