package br.api.universum.identity.application;

import br.api.universum.identity.application.port.UnitOfWorkPort;

import java.util.function.Supplier;

/** Executa o trabalho na hora, sem transação. Para os testes unitários dos casos de uso. */
public class ImmediateUnitOfWork implements UnitOfWorkPort {

    @Override
    public <T> T execute(Supplier<T> work) {
        return work.get();
    }

    @Override
    public void run(Runnable work) {
        work.run();
    }

    @Override
    public <T> T read(Supplier<T> query) {
        return query.get();
    }
}
