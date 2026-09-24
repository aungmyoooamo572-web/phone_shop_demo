package com.example.demo.user.controller;

import com.example.demo.user.dto.LoginDto;
import com.example.demo.user.dto.LoginResponseDto;
import com.example.demo.user.dto.UserCreateDto;
import com.example.demo.user.dto.UserResponseDto;
import com.example.demo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@Valid @RequestBody UserCreateDto dto) {
        return userService.register(dto);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginDto dto) {
        return userService.login(dto);
    }

}
