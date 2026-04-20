package com.conciliacao.coletor.exception;

/**
 * Thrown when the establishment's Conciflex credentials are rejected.
 * Results in status 'login_failed' in LogColeta.
 */
public class LoginConciflexException extends RuntimeException {

    public LoginConciflexException(String estabelecimento, String motivo) {
        super("Login failed for establishment '" + estabelecimento + "': " + motivo);
    }

    public LoginConciflexException(String estabelecimento, String motivo, Throwable cause) {
        super("Login failed for establishment '" + estabelecimento + "': " + motivo, cause);
    }
}
