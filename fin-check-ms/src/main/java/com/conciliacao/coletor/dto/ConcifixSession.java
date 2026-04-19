package com.conciliacao.coletor.dto;

/**
 * Cookies e token de sessão extraídos após o fluxo de login no Conciflex.
 * Usados como headers nas chamadas HTTP às APIs de coleta.
 * NUNCA logar nenhum desses valores.
 */
public record ConcifixSession(
    String laravelSession,
    String xsrfToken,   // valor já URL-decoded do cookie XSRF-TOKEN
    String cfClearance, // cookie do Cloudflare
    String csrfToken    // valor do campo _token do formulário Laravel (meta tag csrf-token)
) {}
