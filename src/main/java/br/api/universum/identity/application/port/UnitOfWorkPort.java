package br.api.universum.identity.application.port;

import java.util.function.Supplier;

/**
 * Fronteira transacional de um caso de uso.
 *
 * <p>Tudo o que é feito dentro do bloco é commitado ou desfeito junto. Uma
 * {@link br.api.universum.identity.domain.exception.DomainException} é um resultado esperado, então
 * ela <strong>commita</strong> o que foi gravado antes dela e depois é relançada: um login com senha
 * errada mantém o contador de tentativas, e o reuso de um refresh token revogado mantém a revogação
 * de todas as sessões. Qualquer outra exceção desfaz o bloco (rollback).</p>
 */
public interface UnitOfWorkPort {

    <T> T execute(Supplier<T> work);

    void run(Runnable work);

    /** Igual a {@link #execute(Supplier)}, para blocos que só fazem leitura. */
    <T> T read(Supplier<T> query);
}
