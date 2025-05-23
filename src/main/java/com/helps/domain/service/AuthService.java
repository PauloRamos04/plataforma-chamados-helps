package com.helps.domain.service;

import com.helps.domain.model.Role;
import com.helps.dto.LoginRequest;
import com.helps.dto.LoginResponse;
import com.helps.domain.model.User;
import com.helps.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private ActivityLogService activityLogService;

    public LoginResponse authenticate(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos!"));

        if (!user.isLoginCorrect(loginRequest, bCryptPasswordEncoder)) {
            HttpServletRequest request = getCurrentRequest();
            activityLogService.logActivity(user, "LOGIN_FAILED", request, "Tentativa de login com senha incorreta");
            throw new BadCredentialsException("Usuário ou senha inválidos!");
        }

        if (!user.isEnabled()) {
            HttpServletRequest request = getCurrentRequest();
            activityLogService.logActivity(user, "LOGIN_DISABLED", request, "Tentativa de login de usuário desabilitado");
            throw new BadCredentialsException("Usuário desabilitado!");
        }

        HttpServletRequest request = getCurrentRequest();
        String sessionId = activityLogService.createUserSession(user, request);

        var now = Instant.now();
        var expireIn = 3600L;

        var roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        var claims = JwtClaimsSet.builder()
                .issuer("helps-platform")
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expireIn))
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new LoginResponse(jwtValue, expireIn);
    }

    public void logout(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            activityLogService.endUserSession(sessionId);
        } else {
            System.err.println("SessionId vazio ou nulo no AuthService.logout");
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}