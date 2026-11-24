package br.api.universum.identity.infrastructure.persistence;

import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.domain.exception.DomainException;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * {@link UnitOfWorkPort} sobre as transações do Spring. Participa da transação atual, se houver.
 *
 * <p>Uma {@link DomainException} é capturada dentro da transação, para que ela seja commitada, e
 * relançada em seguida (equivale a {@code @Transactional(noRollbackFor = DomainException.class)}).
 * Qualquer outra exceção faz rollback.</p>
 */
@Component
public class TransactionalUnitOfWork implements UnitOfWorkPort {

    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readTransaction;

    public TransactionalUnitOfWork(PlatformTransactionManager transactionManager) {
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return inTransaction(writeTransaction, work);
    }

    @Override
    public void run(Runnable work) {
        inTransaction(writeTransaction, () -> {
            work.run();
            return null;
        });
    }

    @Override
    public <T> T read(Supplier<T> query) {
        return inTransaction(readTransaction, query);
    }

    private static <T> T inTransaction(TransactionTemplate transaction, Supplier<T> work) {
        Outcome<T> outcome = transaction.execute(status -> {
            try {
                return new Outcome<>(work.get(), null);
            } catch (DomainException rejection) {
                return new Outcome<>(null, rejection);
            }
        });
        if (outcome.rejection() != null) {
            throw outcome.rejection();
        }
        return outcome.value();
    }

    private record Outcome<T>(T value, DomainException rejection) {
    }
}
