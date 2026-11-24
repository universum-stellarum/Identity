package br.api.universum.identity.domain.exception;

/** Indica que a conta do usuário está temporariamente bloqueada por excesso de tentativas de login falhas. */
public class UserBlockedException extends BusinessException {

    public UserBlockedException() {
        super("Account temporarily locked due to multiple failed attempts. Try again later.");
    }
}
