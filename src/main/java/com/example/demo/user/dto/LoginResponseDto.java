package com.example.demo.user.dto;

import com.example.demo.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private UUID id;
    private String username;
    private String email;
    private String phone;
    private String address;
    private Role role;
    private String token;

}
