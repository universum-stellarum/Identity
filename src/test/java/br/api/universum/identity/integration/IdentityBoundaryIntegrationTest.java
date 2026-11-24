package br.api.universum.identity.integration;

import br.api.universum.identity.application.dto.LoginCommand;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.application.usecase.LoginUseCase;
import br.api.universum.identity.domain.model.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityBoundaryIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepositoryPort users;
    @Autowired PasswordEncoder passwords;
    @Autowired LoginUseCase login;

    @Test
    void amIWorkingIsPublicAndIgnoresAStaleAccessToken() throws Exception {
        for (RequestBuilder request : new RequestBuilder[]{
                get("/identity/amiworking"), get("/identity/amiworking").header("Authorization", "Bearer expired-access-token")}) {
            mvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("identity: application running"));
        }
    }

    @Test
    void publishesOnlyThePublicKeyAndItVerifiesIssuedAccessTokens() throws Exception {
        var user = users.save(User.register("jwks-boundary@universum.api.br", passwords.encode("secret123"), "JWKS"));
        var tokens = login.execute(new LoginCommand(user.getEmail(), "secret123"));
        var response = mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist())
                .andExpect(jsonPath("$.keys[0].q").doesNotExist())
                .andReturn();
        RSAKey publicKey = (RSAKey) JWKSet.parse(response.getResponse().getContentAsString()).getKeys().getFirst();
        assertThat(publicKey.isPrivate()).isFalse();
        var decoded = NimbusJwtDecoder.withPublicKey(publicKey.toRSAPublicKey()).build().decode(tokens.accessToken());
        assertThat(decoded.getSubject()).isEqualTo(user.getId().toString());
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("https://production.universum.api.br");
        assertThat(decoded.getHeaders()).containsEntry("kid", publicKey.getKeyID());
    }

    @Test
    void refreshAndLogoutWorkEvenWithAnInvalidAccessTokenHeader() throws Exception {
        users.save(User.register("refresh-boundary@universum.api.br", passwords.encode("secret123"), "Refresh"));
        var loginResult = mvc.perform(post("/identity/api/v1/auth/login")
                        .header("Authorization", "Bearer expired-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"refresh-boundary@universum.api.br\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn();
        var mapper = new ObjectMapper();
        String refresh = mapper.readTree(loginResult.getResponse().getContentAsString()).get("refreshToken").asText();
        var result = mvc.perform(post("/identity/api/v1/auth/refresh")
                        .header("Authorization", "Bearer expired-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String rotated = mapper.readTree(result.getResponse().getContentAsString()).get("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(refresh);
        mvc.perform(post("/identity/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/identity/api/v1/auth/logout").header("Authorization", "Bearer expired-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + rotated + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/identity/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotated + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/identity/api/v1/users/me").header("Authorization", "Bearer " + rotated))
                .andExpect(status().isUnauthorized());
    }
}
