package br.api.universum.identity.presentation.controller;

import br.api.universum.identity.application.usecase.GetVerificationKeyUseCase;
import br.api.universum.identity.presentation.dto.response.JwkSetResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final GetVerificationKeyUseCase verificationKey;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> publicKeys() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(JwkSetResponse.from(verificationKey.execute()));
    }
}
