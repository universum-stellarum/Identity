package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepositoryPort users;
    private final UnitOfWorkPort unitOfWork;

    public PageResult<UserResult> execute(PageQuery query) {
        return unitOfWork.read(() -> users.findAll(query).map(UserResult::from));
    }
}
