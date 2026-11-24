package br.api.universum.identity.integration;

import br.api.universum.identity.application.abstractions.AccessTokenIssuer;
import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.application.abstractions.TokenHasher;
import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.application.usecases.auth.RefreshSessionUseCase;
import br.api.universum.identity.domain.exception.InvalidRefreshTokenException;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;
import br.api.universum.identity.application.usecases.auth.LoginRequest;
import br.api.universum.identity.application.usecases.auth.RefreshTokenRequest;
import br.api.universum.identity.application.usecases.users.RegisterUserRequest;
import br.api.universum.identity.application.usecases.users.UpdateUserRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthenticationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccessTokenIssuer accessTokens;
    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private TokenHasher tokenHasher;
    @Autowired private RefreshSessionUseCase refreshSession;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // O cadastro de usuário é protegido, então os testes precisam de um JWT válido para criar usuários
    private String registrarAuthHeader;

    @BeforeEach
    void setUpRegistrarToken() {
        User registrar = User.restore(UUID.randomUUID(), "registrar@universum.api.br", "unused", "Registrar",
                User.DEFAULT_ROLE, 0, null);
        registrarAuthHeader = "Bearer " + accessTokens.issue(registrar).value();
    }

    @Test
    void shouldPerformFullAuthenticationLifecycle() throws Exception {
        // 1. Cadastra um usuário
        RegisterUserRequest registerRequest = new RegisterUserRequest("test@universum.api.br", "secret123", "John Doe");

        mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("test@universum.api.br")))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.roles", is("ROLE_USER")));

        // 2. Tenta acessar /identity/api/v1/users/me sem token (deve retornar 401 Unauthorized)
        mockMvc.perform(get("/identity/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        // 3. Faz login e obtém o par de tokens
        LoginRequest loginRequest = new LoginRequest("test@universum.api.br", "secret123");

        MvcResult loginResult = mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseContent).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(responseContent).get("refreshToken").asText();

        // 4. Acessa /identity/api/v1/users/me com o access token (deve retornar os dados do usuário)
        mockMvc.perform(get("/identity/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("test@universum.api.br")))
                .andExpect(jsonPath("$.name", is("John Doe")));

        // 5. Usa o refresh token para obter novos tokens
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/identity/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn();

        String newResponseContent = refreshResult.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(newResponseContent).get("accessToken").asText();

        // 6. Tenta usar o refresh token antigo de novo (deve falhar por causa da rotação/revogação)
        mockMvc.perform(post("/identity/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());

        // 7. Usa o novo access token para chamar o endpoint protegido
        mockMvc.perform(get("/identity/api/v1/users/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("test@universum.api.br")));
    }

    @Test
    void shouldLockAccountAfterFiveFailedAttempts() throws Exception {
        // 1. Cadastra o usuário
        RegisterUserRequest registerRequest = new RegisterUserRequest("lock@universum.api.br", "secret123", "Locked User");

        mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Tenta logar com a senha errada 4 vezes seguidas (ainda não deve bloquear)
        LoginRequest wrongLoginRequest = new LoginRequest("lock@universum.api.br", "wrongpassword");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/identity/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", is("Invalid credentials.")));
        }

        // 3. A 5ª tentativa falha deve disparar o bloqueio temporário
        mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("locked")));

        // 4. Tenta logar com a senha CORRETA (deve continuar bloqueado)
        LoginRequest correctLoginRequest = new LoginRequest("lock@universum.api.br", "secret123");

        mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLoginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("locked")));
    }

    @Test
    void shouldPerformFullUserCrud() throws Exception {
        // 1. Cadastra um admin e obtém um token para autorizar os endpoints do CRUD
        RegisterUserRequest adminRegister = new RegisterUserRequest("admin@universum.api.br", "admin123", "Admin User");

        mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegister)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@universum.api.br", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
        String authHeader = "Bearer " + token;

        // 2. Cria um novo usuário pelo endpoint do CRUD (cadastrando outro usuário)
        RegisterUserRequest userRequest = new RegisterUserRequest("crud@universum.api.br", "crud123", "Crud User");

        MvcResult createResult = mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String newUserId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 3. Busca o usuário pelo id
        mockMvc.perform(get("/identity/api/v1/users/" + newUserId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("crud@universum.api.br")))
                .andExpect(jsonPath("$.name", is("Crud User")));

        // 4. Lista todos os usuários (paginado)
        mockMvc.perform(get("/identity/api/v1/users")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

        // 5. Atualiza os dados do usuário
        UpdateUserRequest updateRequest = new UpdateUserRequest("crud_updated@universum.api.br", "Crud User Updated", "ROLE_ADMIN");

        mockMvc.perform(put("/identity/api/v1/users/" + newUserId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Crud User Updated")))
                .andExpect(jsonPath("$.email", is("crud_updated@universum.api.br")))
                .andExpect(jsonPath("$.roles", is("ROLE_ADMIN")));

        // 6. Remove o usuário pelo id
        mockMvc.perform(delete("/identity/api/v1/users/" + newUserId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        // 7. Busca o usuário removido (deve retornar 404 Not Found pela nossa exceção)
        mockMvc.perform(get("/identity/api/v1/users/" + newUserId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("User not found.")));
    }

    @Test
    void shouldRejectInvalidPayloadsWithBadRequest() throws Exception {
        // 1. Cadastro com senha em branco e e-mail inválido
        mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("invalid", " ", "John"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", is("Invalid email.")))
                .andExpect(jsonPath("$.errors.password", is("Password is required.")));

        // 2. Login sem senha
        mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("john@universum.api.br", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", is("Password is required.")));

        // 3. Refresh sem token
        mockMvc.perform(post("/identity/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.refreshToken", is("Refresh token is required.")));

        // 4. Atualização com campos faltando (exige autenticação)
        mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("valid@universum.api.br", "secret123", "Valid User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("valid@universum.api.br", "secret123"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
        String userId = objectMapper.readTree(
                mockMvc.perform(get("/identity/api/v1/users/me").header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString()
        ).get("id").asText();

        mockMvc.perform(put("/identity/api/v1/users/" + userId)
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, " ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", is("Email is required.")))
                .andExpect(jsonPath("$.errors.name", is("Name is required.")))
                .andExpect(jsonPath("$.errors.roles", is("Roles are required.")));
    }

    @Test
    void shouldRejectMalformedRequestsWithBadRequest() throws Exception {
        // 1. JSON quebrado no corpo
        mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"john@universum.api.br\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Missing or malformed request body.")));

        // 2. Id que não é UUID no caminho (exige autenticação)
        mockMvc.perform(post("/identity/api/v1/users/register")
                        .header("Authorization", registrarAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("path@universum.api.br", "secret123", "Path User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/identity/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("path@universum.api.br", "secret123"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/identity/api/v1/users/abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Parameter 'id' has an invalid value.")));
    }

    @Test
    void authenticatedUserCanListUsersAndUpdateOwnProfile() throws Exception {
        User user = users.save(User.register("regular@universum.api.br", "unused", "Regular"));
        String auth = "Bearer " + accessTokens.issue(user).value();
        mockMvc.perform(get("/identity/api/v1/users").header("Authorization", auth)).andExpect(status().isOk());
        mockMvc.perform(put("/identity/api/v1/users/me").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"regular@universum.api.br\",\"name\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Updated")));
    }

    @Test
    void onlyOneConcurrentRefreshCanConsumeTheToken() throws Exception {
        User user = users.save(User.register("concurrent@universum.api.br", "unused", "Concurrent"));
        refreshTokens.save(RefreshToken.issue(
                tokenHasher.hash("concurrent-refresh"), user.getId(), Instant.now(), Duration.ofDays(1)));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                start.await();
                try {
                    refreshSession.execute(new RefreshTokenRequest("concurrent-refresh"));
                    return true;
                } catch (InvalidRefreshTokenException expected) {
                    return false;
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void logoutRevokesRefreshTokenAndIsIdempotent() throws Exception {
        User user = users.save(User.register("logout@universum.api.br", "unused", "Logout"));
        refreshTokens.save(RefreshToken.issue(
                tokenHasher.hash("logout-refresh"), user.getId(), Instant.now(), Duration.ofDays(1)));
        String body = "{\"refreshToken\":\"logout-refresh\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/identity/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(post("/identity/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
