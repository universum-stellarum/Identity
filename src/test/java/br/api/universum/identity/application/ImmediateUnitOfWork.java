package br.api.universum.identity.application;

import br.api.universum.identity.application.port.UnitOfWorkPort;

import java.util.function.Supplier;

/** Runs the work right away, without a transaction. For use case unit tests. */
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
