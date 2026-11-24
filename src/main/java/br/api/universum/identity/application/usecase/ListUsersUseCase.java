package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepository users;
    private final UnitOfWork unitOfWork;

    public PageResult<UserResult> execute(PageQuery query) {
        return unitOfWork.read(() -> users.findAll(query).map(UserResult::from));
    }
}
