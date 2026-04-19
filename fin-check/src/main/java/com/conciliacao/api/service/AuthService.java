package com.conciliacao.api.service;

import com.conciliacao.api.config.JwtConfig;
import com.conciliacao.api.dto.request.LoginRequest;
import com.conciliacao.api.dto.response.JwtResponse;
import com.conciliacao.api.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;

    public JwtResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.login(), request.senha())
        );

        User user = (User) auth.getPrincipal();
        log.info("Login realizado com sucesso: {}", user.getLogin());

        String token = jwtConfig.gerarToken(user.getLogin(), user.getRole());
        return new JwtResponse(token, jwtConfig.getExpirationMs());
    }
}
