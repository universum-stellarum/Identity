package br.api.universum.identity.infrastructure.security.jwt;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.UUID;

/**
 * A chave ativa já convertida para uso: o encoder que assina e o decoder que valida.
 *
 * @param id o {@code kid} da chave
 */
record JwtKeyMaterial(UUID id, JwtEncoder encoder, JwtDecoder decoder) {
}
