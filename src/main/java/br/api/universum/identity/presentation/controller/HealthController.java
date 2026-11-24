package br.api.universum.identity.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health(HttpServletRequest request) {
        request.setAttribute("skipHealthAccessLog", Boolean.TRUE);
        return Map.of("status", "UP");
    }

    /** Página de teste para conferir no navegador que o gateway está roteando até o identity. */
    @GetMapping(value = "/identity/amiworking", produces = MediaType.TEXT_PLAIN_VALUE)
    public String amIWorking() {
        return "identity: application running";
    }
}
