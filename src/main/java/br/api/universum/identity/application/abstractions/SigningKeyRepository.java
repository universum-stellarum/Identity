package br.api.universum.identity.application.abstractions;

import br.api.universum.identity.domain.model.SigningKey;

import java.util.Optional;

public interface SigningKeyRepository {

    /** A chave ativa, ou vazio enquanto nenhum par foi gerado. */
    Optional<SigningKey> findActive();

    /**
     * Carrega a chave, inicializada ou não, e a bloqueia até o fim da unidade de trabalho. Isso
     * serializa a inicialização e a rotação entre todas as instâncias do serviço. Vazio quando o
     * registro da chave não existe (migration ausente).
     */
    Optional<SigningKey> findForUpdate();

    <S extends SigningKey> S save(S key);
}
