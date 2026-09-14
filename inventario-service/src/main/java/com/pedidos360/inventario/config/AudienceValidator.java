package com.pedidos360.inventario.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Azure AD puede emitir el "aud" del token como el Client ID plano o como
 * "api://{clientId}" según cómo se pidió el scope. Aceptamos ambas formas.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audiencia;

    public AudienceValidator(String audiencia) {
        this.audiencia = audiencia;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience().contains(audiencia) || jwt.getAudience().contains("api://" + audiencia)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "El token no fue emitido para esta API (audience incorrecto)", null));
    }
}
