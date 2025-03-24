package com.helps.controller;

import com.helps.dto.LoginRequest;
import com.helps.dto.LoginResponse;
import com.helps.domain.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "https://helps-plataforms-frontend.vercel.app"
        },
        allowCredentials = "true",
        methods = {RequestMethod.POST, RequestMethod.OPTIONS}
)
public class TokenController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }
}