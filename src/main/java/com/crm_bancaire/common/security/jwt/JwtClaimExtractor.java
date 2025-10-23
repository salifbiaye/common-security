package com.crm_bancaire.common.security.jwt;

import com.crm_bancaire.common.security.context.UserContext.ActorInfo;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Interface pour extraire les informations utilisateur depuis un JWT.
 *
 * Cette interface permet de supporter différents providers d'authentification
 * (Keycloak, Auth0, Okta, custom JWT, etc.) en créant une implémentation custom.
 *
 * Par défaut, {@link KeycloakJwtClaimExtractor} est utilisé.
 *
 * Pour utiliser un autre provider, créez une implémentation de cette interface
 * et annotez-la avec @Component. Elle sera automatiquement détectée et utilisée.
 *
 * Exemple avec Auth0:
 * <pre>
 * &#64;Component
 * public class Auth0JwtClaimExtractor implements JwtClaimExtractor {
 *     &#64;Override
 *     public ActorInfo extractFromJwt(Jwt jwt) {
 *         return ActorInfo.builder()
 *             .sub(jwt.getClaimAsString("sub"))
 *             .email(jwt.getClaimAsString("email"))
 *             .username(jwt.getClaimAsString("nickname"))  // Auth0 utilise "nickname"
 *             .firstName(jwt.getClaimAsString("given_name"))
 *             .lastName(jwt.getClaimAsString("family_name"))
 *             .role(extractAuth0Role(jwt))
 *             .build();
 *     }
 *
 *     private String extractAuth0Role(Jwt jwt) {
 *         // Auth0 met les rôles dans un namespace custom
 *         List&lt;String&gt; roles = jwt.getClaimAsStringList("https://myapp.com/roles");
 *         return roles != null && !roles.isEmpty() ? roles.get(0) : "USER";
 *     }
 * }
 * </pre>
 */
public interface JwtClaimExtractor {

    /**
     * Extrait les informations utilisateur depuis un objet Jwt (SecurityContext).
     *
     * Utilisé lorsque le JWT est déjà parsé par Spring Security.
     *
     * @param jwt Le JWT parsé par Spring Security
     * @return Les informations de l'utilisateur, ou null si l'extraction échoue
     */
    ActorInfo extractFromJwt(Jwt jwt);

    /**
     * Extrait les informations utilisateur depuis un token JWT brut (String).
     *
     * Utilisé pour les appels inter-services (Feign) où le JWT est dans le header Authorization.
     *
     * @param token Le token JWT brut (sans "Bearer ")
     * @return Les informations de l'utilisateur, ou null si l'extraction échoue
     */
    ActorInfo extractFromToken(String token);
}
