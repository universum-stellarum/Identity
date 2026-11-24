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
@Tag(name = "Users", description = "Endpoints for managing (CRUD) users in the system")
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
            summary = "Register a new user",
            description = "Creates a new regular user in the database. Requires JWT authentication."
    )
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Email already in use or invalid payload")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(registerUser.execute(request.toCommand()));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user data",
            description = "Retrieves the profile details of the currently authenticated user from the JWT."
    )
    @ApiResponse(responseCode = "200", description = "Data retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized / missing or invalid JWT token")
    public UserResponse me(@CurrentUserId UUID userId) {
        return UserResponse.from(getUser.execute(userId));
    }

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns a page with registered users. Requires JWT authentication."
    )
    @ApiResponse(responseCode = "200", description = "Page returned successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public Page<UserResponse> listAll(@PageableDefault(size = 20) Pageable pageable) {
        PageResult<UserResponse> page = listUsers.execute(toQuery(pageable)).map(UserResponse::from);
        return new PageImpl<>(page.content(), pageable, page.totalElements());
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile", description = "Updates name and email without changing roles.")
    public UserResponse updateProfile(@CurrentUserId UUID userId,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateProfile.execute(userId, request.toCommand()));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Find user by ID",
            description = "Returns the details of a specific user by their unique ID."
    )
    @ApiResponse(responseCode = "200", description = "User found successfully")
    @ApiResponse(responseCode = "400", description = "User not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public UserResponse getById(@PathVariable UUID id) {
        return UserResponse.from(getUser.execute(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user data",
            description = "Modifies the email, name, and roles of an existing user."
    )
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid ID, email already in use, or inconsistent data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(updateUser.execute(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a user",
            description = "Physically removes a specific user's record by their unique ID."
    )
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "400", description = "User not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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
