package com.eshoppingzone.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Integer profileId;
    private String fullName;
    private String emailId;
    private String password;
    private Long mobileNumber;
    private String gender;
    private String role;
    private String provider;
    private String about;
}
