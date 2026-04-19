package com.conciliacao.coletor.exception;

/**
 * Lançada quando o login/senha do estabelecimento é rejeitado pelo Conciflex.
 * Resulta em status 'login_failed' no LogColeta.
 */
public class LoginConciflexException extends RuntimeException {

    public LoginConciflexException(String estabelecimento, String motivo) {
        super("Login falhou para estabelecimento '" + estabelecimento + "': " + motivo);
    }
}
