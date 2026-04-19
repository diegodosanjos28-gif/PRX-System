package com.conciliacao.coletor.exception;

/**
 * Lançada quando a coleta falha por razão técnica (timeout, erro de rede, erro de parsing).
 * Resulta em status 'timeout' ou 'error' no LogColeta dependendo da exceção original.
 */
public class ColetaException extends RuntimeException {

    public ColetaException(String message) {
        super(message);
    }

    public ColetaException(String message, Throwable cause) {
        super(message, cause);
    }
}
