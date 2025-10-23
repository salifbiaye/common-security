package com.crm_bancaire.common.security.jwt;

import com.crm_bancaire.common.security.context.UserContext.ActorInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Implémentation par défaut pour extraire les claims depuis un JWT Keycloak.
 *
 * Cette classe extrait les informations utilisateur selon la structure standard de Keycloak:
 * - Claims standards: sub, email, preferred_username, given_name, family_name
 * - Rôles: resource_access.oauth2-pkce.roles[0]
 *
 * Cette implémentation est automatiquement utilisée par défaut via UserContextAutoConfiguration.
 *
 * Pour utiliser un autre provider d'authentification, créez votre propre @Component
 * qui implémente {@link JwtClaimExtractor} et il remplacera automatiquement celui-ci.
 */
@Slf4j
public class KeycloakJwtClaimExtractor implements JwtClaimExtractor {

    private static final String DEFAULT_CLIENT_NAME = "oauth2-pkce";
    private static final String DEFAULT_ROLE = "USER";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ActorInfo extractFromJwt(Jwt jwt) {
        try {
            return ActorInfo.builder()
                .sub(jwt.getClaimAsString("sub"))
                .email(jwt.getClaimAsString("email"))
                .username(jwt.getClaimAsString("preferred_username"))
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .role(extractRoleFromJwt(jwt))
                .build();
        } catch (Exception e) {
            log.warn("⚠️ Failed to extract actor info from Jwt object: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ActorInfo extractFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("JWT format invalide - doit avoir 3 parties (header.payload.signature)");
                return null;
            }

            // Décoder le payload (partie 2)
            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            JsonNode node = objectMapper.readTree(decodedPayload);

            return ActorInfo.builder()
                .sub(getClaimAsString(node, "sub"))
                .email(getClaimAsString(node, "email"))
                .username(getClaimAsString(node, "preferred_username"))
                .firstName(getClaimAsString(node, "given_name"))
                .lastName(getClaimAsString(node, "family_name"))
                .role(extractRoleFromNode(node))
                .build();

        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de l'extraction du JWT depuis le token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrait le rôle depuis l'objet Jwt (SecurityContext).
     *
     * Structure Keycloak: resource_access.{client-name}.roles[0]
     */
    private String extractRoleFromJwt(Jwt jwt) {
        try {
            Object resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof Map<?, ?> resourceMap) {
                // Essayer d'abord avec le client name par défaut
                Object clientData = resourceMap.get(DEFAULT_CLIENT_NAME);
                if (clientData instanceof Map<?, ?> clientMap) {
                    Object roles = clientMap.get("roles");
                    if (roles instanceof List<?> rolesList && !rolesList.isEmpty()) {
                        return rolesList.get(0).toString();
                    }
                }
            }
            return DEFAULT_ROLE;
        } catch (Exception e) {
            log.debug("Could not extract role from JWT, using default: {}", DEFAULT_ROLE);
            return DEFAULT_ROLE;
        }
    }

    /**
     * Extrait le rôle depuis un JsonNode (token brut).
     */
    private String extractRoleFromNode(JsonNode node) {
        try {
            JsonNode resourceAccess = node.get("resource_access");
            if (resourceAccess != null) {
                JsonNode clientData = resourceAccess.get(DEFAULT_CLIENT_NAME);
                if (clientData != null) {
                    JsonNode roles = clientData.get("roles");
                    if (roles != null && roles.isArray() && roles.size() > 0) {
                        return roles.get(0).asText();
                    }
                }
            }
            return DEFAULT_ROLE;
        } catch (Exception e) {
            log.debug("Could not extract role from JWT node, using default: {}", DEFAULT_ROLE);
            return DEFAULT_ROLE;
        }
    }

    /**
     * Extrait un claim String depuis un JsonNode.
     */
    private String getClaimAsString(JsonNode node, String claimName) {
        JsonNode claimNode = node.get(claimName);
        return claimNode != null && !claimNode.isNull() ? claimNode.asText() : null;
    }
}
