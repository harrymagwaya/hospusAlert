package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.Gender;
import com.shanalert.hospitalalert.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Gender gender;
    private UserStatus userStatus;
}