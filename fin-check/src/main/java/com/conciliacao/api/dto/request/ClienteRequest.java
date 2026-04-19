package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 255, message = "Razão social não pode exceder 255 caracteres")
    String razaoSocial,

    @Size(max = 255, message = "Nome fantasia não pode exceder 255 caracteres")
    String nomeFantasia,

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter exatamente 14 dígitos numéricos (sem pontuação)")
    String cnpj,

    @NotBlank(message = "WhatsApp é obrigatório")
    @Pattern(regexp = "\\d{10,15}", message = "WhatsApp deve conter entre 10 e 15 dígitos numéricos (com DDD e DDI, sem pontuação)")
    String whatsapp,

    @NotBlank(message = "Login Conciflex é obrigatório")
    String conciflexLogin,

    @NotBlank(message = "Senha Conciflex é obrigatória")
    String conciflexSenha,

    String observacoes

) {}
