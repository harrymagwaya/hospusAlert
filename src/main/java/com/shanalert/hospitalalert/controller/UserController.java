package com.shanalert.hospitalalert.controller;


import com.shanalert.hospitalalert.dto.UserUpdateDTO;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/{id}")
    public User patchUser(
            @PathVariable UUID id,
            @RequestBody UserUpdateDTO updateDto) {
        return userService.updateUser(id, updateDto);
    }
}
