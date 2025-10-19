package com.crm_bancaire.common.security.metadata;

import com.crm_bancaire.common.security.dto.SecurityRules;
import com.crm_bancaire.common.security.scanner.SecurityRulesScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller fournis par la librairie common-security pour exposer
 * les règles de sécurité d'un microservice. L'enregistrement est
 * conditionnel : il s'active seulement si la propriété
 * `common.security.expose-metadata=true` est définie et qu'un
 * `SecurityRulesScanner` est présent.
 */
@RestController
@RequestMapping("/security")
@ConditionalOnProperty(prefix = "common.security", name = "expose-metadata", havingValue = "true")
@ConditionalOnBean(SecurityRulesScanner.class)
@Slf4j
public class SecurityMetadataController {

    private final SecurityRulesScanner scanner;

    public SecurityMetadataController(SecurityRulesScanner scanner) {
        this.scanner = scanner;
    }

    @GetMapping("/rules")
    public SecurityRules getRules() {
        SecurityRules rules = scanner.getSecurityRules();
        log.info("📋 Security rules requested - returning {} endpoints",
                 rules != null && rules.getEndpoints() != null ? rules.getEndpoints().size() : 0);
        return rules;
    }
}
