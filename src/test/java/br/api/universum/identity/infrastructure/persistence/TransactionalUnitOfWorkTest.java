package br.api.universum.identity.infrastructure.persistence;

import br.api.universum.identity.domain.exception.InvalidCredentialsException;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalUnitOfWorkTest {

    private final RecordingTransactionManager transactions = new RecordingTransactionManager();
    private final TransactionalUnitOfWork unitOfWork = new TransactionalUnitOfWork(transactions);

    @Test
    void commitsAndReturnsTheResult() {
        assertThat(unitOfWork.execute(() -> "done")).isEqualTo("done");
        assertThat(transactions.outcomes).containsExactly("commit");
    }

    @Test
    void domainExceptionsCommitWhatWasRecordedAndAreRethrown() {
        assertThatThrownBy(() -> unitOfWork.run(() -> {
            throw new InvalidCredentialsException();
        })).isInstanceOf(InvalidCredentialsException.class);

        assertThat(transactions.outcomes).containsExactly("commit");
    }

    @Test
    void otherExceptionsRollBack() {
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            throw new IllegalStateException("database failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(transactions.outcomes).containsExactly("rollback");
    }

    @Test
    void readsUseAReadOnlyTransaction() {
        unitOfWork.read(() -> null);

        assertThat(transactions.readOnly).containsExactly(true);
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private final List<String> outcomes = new ArrayList<>();
        private final List<Boolean> readOnly = new ArrayList<>();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            readOnly.add(definition.isReadOnly());
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            outcomes.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            outcomes.add("rollback");
        }
    }
}
