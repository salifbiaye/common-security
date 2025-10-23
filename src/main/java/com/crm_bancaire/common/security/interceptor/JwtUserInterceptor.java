package com.crm_bancaire.common.security.interceptor;

import com.crm_bancaire.common.security.context.UserContext;
import com.crm_bancaire.common.security.context.UserContext.ActorInfo;
import com.crm_bancaire.common.security.jwt.JwtClaimExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor qui extrait automatiquement les informations de l'utilisateur depuis le JWT
 * et les stocke dans {@link UserContext} pour un accès facile partout dans l'application.
 *
 * Cet interceptor utilise deux stratégies pour extraire le JWT:
 * 1. **SecurityContext** (requêtes directes depuis le Gateway) - le JWT est déjà parsé
 * 2. **Authorization header** (appels inter-services via Feign) - parse manuellement le JWT
 *
 * L'extraction des claims est déléguée à {@link JwtClaimExtractor}, ce qui permet
 * de supporter différents providers d'authentification.
 *
 * Le contexte est automatiquement nettoyé à la fin de chaque requête.
 */
@Slf4j
public class JwtUserInterceptor implements HandlerInterceptor {

    private final JwtClaimExtractor jwtClaimExtractor;

    public JwtUserInterceptor(JwtClaimExtractor jwtClaimExtractor) {
        this.jwtClaimExtractor = jwtClaimExtractor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Stratégie 1: Extraction depuis SecurityContext (requêtes directes)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                ActorInfo actor = jwtClaimExtractor.extractFromJwt(jwt);
                if (actor != null) {
                    UserContext.setCurrentActor(actor);
                    log.debug("🔑 Current actor set from SecurityContext: {} ({})", actor.getEmail(), actor.getSub());
                    return true;
                }
            }

            // Stratégie 2: Extraction depuis Authorization header (appels Feign)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                ActorInfo actor = jwtClaimExtractor.extractFromToken(token);
                if (actor != null) {
                    UserContext.setCurrentActor(actor);
                    log.debug("🔑 Current actor set from Authorization header: {} ({})", actor.getEmail(), actor.getSub());
                    return true;
                }
            }

            log.debug("⚠️ No JWT found in request - UserContext will be empty");

        } catch (Exception e) {
            log.warn("⚠️ Failed to extract actor info from JWT: {}", e.getMessage());
        }

        return true; // Continuer même si l'extraction échoue
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Nettoyer le contexte pour éviter les fuites de mémoire
        UserContext.clear();
    }
}
