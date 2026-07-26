package com.finserve.controller;

import com.finserve.dto.ApiResponse;
import com.finserve.dto.LoginRequest;
import com.finserve.dto.LoginResponse;
import com.finserve.dto.RegisterRequest;
import com.finserve.dto.LoanApplicationResponseDTO;
import com.finserve.service.LoanService;
import com.finserve.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final LoanService loanService;

    public UserController(UserService userService, LoanService loanService) {
        this.userService = userService;
        this.loanService = loanService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = userService.register(request);
        return new ResponseEntity<>(ApiResponse.success("Registration successful", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/{userId}/loans")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getUserLoans(@PathVariable Long userId) {
        return ResponseEntity.ok(loanService.getUserLoans(userId));
    }
}