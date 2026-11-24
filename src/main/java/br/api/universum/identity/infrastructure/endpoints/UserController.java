package br.api.universum.identity.infrastructure.endpoints;

import br.api.universum.identity.application.usecases.users.DeleteUserUseCase;
import br.api.universum.identity.application.usecases.users.GetUserUseCase;
import br.api.universum.identity.application.usecases.users.ListUsersUseCase;
import br.api.universum.identity.application.usecases.users.RegisterUserUseCase;
import br.api.universum.identity.application.usecases.users.UpdateProfileUseCase;
import br.api.universum.identity.application.usecases.users.UpdateUserUseCase;
import br.api.universum.identity.application.usecases.users.request.RegisterUserRequest;
import br.api.universum.identity.application.usecases.users.request.UpdateProfileRequest;
import br.api.universum.identity.application.usecases.users.request.UpdateUserRequest;
import br.api.universum.identity.application.usecases.users.response.UserResponse;
import br.api.universum.identity.infrastructure.web.CurrentUserId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/identity/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Cadastro, consulta, alteração e remoção de usuários")
public class UserController {

    private final RegisterUserUseCase registerUser;
    private final GetUserUseCase getUser;
    private final ListUsersUseCase listUsers;
    private final UpdateUserUseCase updateUser;
    private final UpdateProfileUseCase updateProfile;
    private final DeleteUserUseCase deleteUser;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastra um novo usuário",
            description = "Cria um usuário comum. Exige autenticação JWT."
    )
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return registerUser.execute(request);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Dados do usuário autenticado",
            description = "Retorna os dados do usuário identificado pelo JWT."
    )
    public UserResponse me(@CurrentUserId UUID userId) {
        return getUser.execute(userId);
    }

    @GetMapping
    @Operation(
            summary = "Lista os usuários",
            description = "Retorna uma página com os usuários cadastrados. Exige autenticação JWT."
    )
    public Page<UserResponse> listAll(@PageableDefault(size = 20) Pageable pageable) {
        return listUsers.execute(pageable);
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza o próprio perfil", description = "Altera nome e e-mail, sem mudar os papéis (roles).")
    public UserResponse updateProfile(@CurrentUserId UUID userId,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return updateProfile.execute(userId, request);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um usuário pelo id",
            description = "Retorna os dados de um usuário pelo id."
    )
    public UserResponse getById(@PathVariable UUID id) {
        return getUser.execute(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um usuário",
            description = "Altera e-mail, nome e papéis (roles) de um usuário existente."
    )
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return updateUser.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove um usuário",
            description = "Remove definitivamente o registro do usuário pelo id."
    )
    public void delete(@PathVariable UUID id) {
        deleteUser.execute(id);
    }
}
