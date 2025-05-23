package com.helps.controller;

import com.helps.dto.LoginRequest;
import com.helps.dto.LoginResponse;
import com.helps.domain.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        LoginResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            System.out.println("=== ENDPOINT LOGOUT CHAMADO ===");
            System.out.println("Request body: " + request);

            String sessionId = request.get("sessionId");
            System.out.println("SessionId extraído: " + sessionId);

            if (sessionId != null && !sessionId.isEmpty()) {
                authService.logout(sessionId);
                System.out.println("Logout processado com sucesso");
                return ResponseEntity.ok(Map.of("success", true, "message", "Logout realizado com sucesso"));
            } else {
                System.err.println("SessionId não fornecido no request");
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "SessionId não fornecido"));
            }
        } catch (Exception e) {
            System.err.println("Erro no endpoint logout: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("success", false, "message", "Erro no logout: " + e.getMessage()));
        }
    }
}