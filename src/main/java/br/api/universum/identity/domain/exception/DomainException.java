package br.api.universum.identity.domain.exception;

/**
 * Base de toda violação de regra lançada pelo domínio ou por um caso de uso.
 *
 * <p>Uma exceção de domínio é um resultado esperado, não uma falha: a unidade de trabalho commita o
 * que o caso de uso gravou antes de lançá-la (tentativas de login falhas, sessões revogadas).</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
