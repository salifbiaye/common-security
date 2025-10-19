package com.crm_bancaire.common.security.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration minimale pour exposer le controller de métadonnées
 * de sécurité quand la propriété `common.security.expose-metadata=true` est activée
 * et qu'un `SecurityRulesScanner` est présent.
 */
@Configuration
@ConditionalOnProperty(prefix = "common.security", name = "expose-metadata", havingValue = "true")
@ConditionalOnBean(name = "securityRulesScanner")
public class SecurityMetadataAutoConfiguration {
    // Le controller est enregistré via @RestController et les conditions ci-dessus
}
