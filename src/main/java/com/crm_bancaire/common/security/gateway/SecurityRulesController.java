package com.crm_bancaire.common.security.gateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/admin/security")
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
