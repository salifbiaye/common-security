package com.crm_bancaire.common.security.metadata;

import com.crm_bancaire.common.security.scanner.SecurityRulesScanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration minimale pour exposer le controller de métadonnées
 * de sécurité quand la propriété `common.security.expose-metadata=true` est activée
 * et qu'un `SecurityRulesScanner` est présent.
 */
@Configuration
@ConditionalOnProperty(prefix = "common.security", name = "expose-metadata", havingValue = "true")
@ConditionalOnBean(SecurityRulesScanner.class)
public class SecurityMetadataAutoConfiguration {

    // Register the controller as a bean so it is available even when the
    // application does not component-scan the library package.
    @Bean
    @ConditionalOnMissingBean
    public SecurityMetadataController securityMetadataController(SecurityRulesScanner scanner) {
        return new SecurityMetadataController(scanner);
    }

}
