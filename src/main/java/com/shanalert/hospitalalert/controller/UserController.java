package com.shanalert.hospitalalert.controller;


import com.shanalert.hospitalalert.dto.UserUpdateDTO;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserStatus;
import com.shanalert.hospitalalert.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Page<User> getAllUsers(
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return userService.getAllUsers(status, pageable);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PatchMapping("/{id}")
    public User patchUser(
            @PathVariable UUID id,
            @RequestBody UserUpdateDTO updateDto) {
        return userService.updateUser(id, updateDto);
    }

    @DeleteMapping("/{id}")
    public void softDeleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}
