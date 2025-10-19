package com.crm_bancaire.common.security.autoconfigure;

import com.crm_bancaire.common.security.scanner.SecurityRulesScanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration qui enregistre des beans utiles de `common-security`
 * pour éviter aux microservices d'avoir à ajouter un `@ComponentScan`.
 *
 * Activation contrôlable via la propriété `common.security.enable-auto-config` (par défaut true).
 */
@Configuration
@ConditionalOnProperty(prefix = "common.security", name = "enable-auto-config", havingValue = "true", matchIfMissing = true)
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityRulesScanner.class)
    public SecurityRulesScanner securityRulesScanner(ApplicationContext applicationContext) {
        // SecurityRulesScanner prend ApplicationContext en constructeur
        return new SecurityRulesScanner(applicationContext);
    }
}
