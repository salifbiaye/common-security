package com.crm_bancaire.common.security.gateway;

import com.crm_bancaire.common.security.dto.EndpointRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authorization Manager qui vérifie dynamiquement les règles de sécurité
 * chargées depuis les microservices
 */
@Slf4j
public class DynamicAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final DynamicSecurityLoader securityLoader;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    public DynamicAuthorizationManager(DynamicSecurityLoader securityLoader) {
        this.securityLoader = securityLoader;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        String path = context.getExchange().getRequest().getPath().value();
        String method = context.getExchange().getRequest().getMethod().name();

        log.debug("🔍 Checking authorization for {} {}", method, path);

        // Récupérer toutes les règles dynamiques
        Map<String, List<EndpointRule>> allRules = securityLoader.getAllRules();
        log.debug("   📋 Loaded rules from services: {}", allRules.keySet());
        log.debug("   📋 Total rules: {}", allRules.values().stream().mapToInt(List::size).sum());

        // Chercher la règle correspondante
        EndpointRule matchingRule = findMatchingRule(allRules, path, method);

        if (matchingRule == null) {
            log.debug("   ❓ No dynamic rule found for {} {} - checking if authenticated", method, path);
            // Pas de règle dynamique trouvée - vérifier juste l'authentification
            return authentication
                .map(auth -> auth.isAuthenticated())
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false));
        }

        // Si l'endpoint est public, autoriser sans authentification
        if (matchingRule.isPublic()) {
            log.debug("   ✅ PUBLIC endpoint {} {} - access granted", method, path);
            return Mono.just(new AuthorizationDecision(true));
        }

        // Vérifier les rôles
        List<String> requiredRoles = matchingRule.getRoles();
        log.debug("   🔒 SECURED endpoint {} {} requires roles: {}", method, path, requiredRoles);

        return authentication
            .filter(Authentication::isAuthenticated)
            .flatMapIterable(Authentication::getAuthorities)
            .map(GrantedAuthority::getAuthority)
            .map(authority -> {
                // Les authorities sont au format "ROLE_XXX"
                if (authority.startsWith("ROLE_")) {
                    return authority.substring(5); // Enlever "ROLE_"
                }
                return authority;
            })
            .collect(Collectors.toSet())
            .map(userRoles -> {
                log.debug("   👤 User roles: {}", userRoles);
                boolean hasRequiredRole = userRoles.stream()
                    .anyMatch(requiredRoles::contains);

                if (hasRequiredRole) {
                    log.debug("   ✅ Access GRANTED for {} {}", method, path);
                } else {
                    log.warn("   ❌ Access DENIED for {} {} - user roles {} do not match required roles {}",
                             method, path, userRoles, requiredRoles);
                }

                return new AuthorizationDecision(hasRequiredRole);
            })
            .defaultIfEmpty(new AuthorizationDecision(false));
    }

    /**
     * Trouve la règle correspondant au path et à la méthode HTTP
     */
    private EndpointRule findMatchingRule(Map<String, List<EndpointRule>> allRules, String path, String method) {
        for (Map.Entry<String, List<EndpointRule>> entry : allRules.entrySet()) {
            for (EndpointRule rule : entry.getValue()) {
                // Vérifier si la méthode HTTP correspond
                if (!rule.getMethods().contains(method)) {
                    continue;
                }

                // Vérifier si le path correspond (support des wildcards et path variables)
                String rulePattern = rule.getFullPath();
                PathPattern pattern = pathPatternParser.parse(rulePattern);
                PathContainer pathContainer = PathContainer.parsePath(path);

                if (pattern.matches(pathContainer)) {
                    log.debug("   ✅ Found matching rule: {} {} → {}", method, rulePattern, rule.getRoles());
                    return rule;
                }
            }
        }
        return null;
    }
}
