package br.api.universum.identity.presentation.controller;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.application.usecase.DeleteUserUseCase;
import br.api.universum.identity.application.usecase.GetUserUseCase;
import br.api.universum.identity.application.usecase.ListUsersUseCase;
import br.api.universum.identity.application.usecase.RegisterUserUseCase;
import br.api.universum.identity.application.usecase.UpdateProfileUseCase;
import br.api.universum.identity.application.usecase.UpdateUserUseCase;
import br.api.universum.identity.presentation.dto.request.RegisterUserRequest;
import br.api.universum.identity.presentation.dto.request.UpdateProfileRequest;
import br.api.universum.identity.presentation.dto.request.UpdateUserRequest;
import br.api.universum.identity.presentation.dto.response.UserResponse;
import br.api.universum.identity.presentation.security.CurrentUserId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(registerUser.execute(request.toCommand()));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Dados do usuário autenticado",
            description = "Retorna os dados do usuário identificado pelo JWT."
    )
    @ApiResponse(responseCode = "200", description = "Dados retornados")
    @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido")
    public UserResponse me(@CurrentUserId UUID userId) {
        return UserResponse.from(getUser.execute(userId));
    }

    @GetMapping
    @Operation(
            summary = "Lista os usuários",
            description = "Retorna uma página com os usuários cadastrados. Exige autenticação JWT."
    )
    @ApiResponse(responseCode = "200", description = "Página retornada")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    public Page<UserResponse> listAll(@PageableDefault(size = 20) Pageable pageable) {
        PageResult<UserResponse> page = listUsers.execute(toQuery(pageable)).map(UserResponse::from);
        return new PageImpl<>(page.content(), pageable, page.totalElements());
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza o próprio perfil", description = "Altera nome e e-mail, sem mudar os papéis (roles).")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    public UserResponse updateProfile(@CurrentUserId UUID userId,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateProfile.execute(userId, request.toCommand()));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um usuário pelo id",
            description = "Retorna os dados de um usuário pelo id."
    )
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    public UserResponse getById(@PathVariable UUID id) {
        return UserResponse.from(getUser.execute(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um usuário",
            description = "Altera e-mail, nome e papéis (roles) de um usuário existente."
    )
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "400", description = "Id ou dados inválidos")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(updateUser.execute(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove um usuário",
            description = "Remove definitivamente o registro do usuário pelo id."
    )
    @ApiResponse(responseCode = "204", description = "Usuário removido")
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    public void delete(@PathVariable UUID id) {
        deleteUser.execute(id);
    }

    private static PageQuery toQuery(Pageable pageable) {
        return new PageQuery(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().stream()
                        .map(order -> new PageQuery.SortOrder(order.getProperty(), order.isAscending()))
                        .toList()
        );
    }
}
