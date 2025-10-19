package com.crm_bancaire.common.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller exposant les endpoints d'administration des règles de sécurité.
 *
 * Important: l'enregistrement du controller est conditionnel à la présence
 * d'un bean `DynamicSecurityLoader`. Cela évite l'instanciation de ce
 * controller dans des applications (ex: user-service) qui ne sont pas des
 * gateways et qui ne fournissent pas le loader.
 */
@RestController
@RequestMapping("/admin/security")
@ConditionalOnBean(DynamicSecurityLoader.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Slf4j
public class SecurityRulesController {

    private final DynamicSecurityLoader securityLoader;

    public SecurityRulesController(DynamicSecurityLoader securityLoader) {
        this.securityLoader = securityLoader;
    }

    /**
     * Endpoint pour recharger manuellement les règles de sécurité
     */

   @GetMapping("/reload")
    public Mono<Map<String, Object>> reloadSecurityRules() {
        log.info("🔄 Manual security rules reload triggered");
        securityLoader.loadSecurityRules();

        return Mono.just(Map.of(
            "message", "Security rules reloaded successfully",
            "services", securityLoader.getAllRules().keySet(),
            "totalRules", securityLoader.getAllRules().values().stream()
                .mapToInt(list -> list.size())
                .sum()
        ));
    }
}
