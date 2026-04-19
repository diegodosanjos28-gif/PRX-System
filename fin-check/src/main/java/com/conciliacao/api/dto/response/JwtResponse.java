package com.conciliacao.api.dto.response;

public record JwtResponse(String token, String tipo, Long expiresIn) {
    public JwtResponse(String token, Long expiresIn) {
        this(token, "Bearer", expiresIn);
    }
}
