package com.crm_bancaire.common.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration pour la sécurité dynamique dans le Gateway
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration")
@Slf4j
public class DynamicSecurityAutoConfiguration {

    @Bean
    @LoadBalanced
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(org.springframework.web.reactive.function.client.WebClient.Builder.class)
    public WebClient.Builder loadBalancedWebClientBuilder() {
        log.info("🔧 Configuring LoadBalanced WebClient.Builder for dynamic security");
        return WebClient.builder();
    }

    @Bean
    public DynamicSecurityLoader dynamicSecurityLoader(
            DiscoveryClient discoveryClient,
            WebClient.Builder webClientBuilder) {

        log.info("🔧 Configuring DynamicSecurityLoader");

        // Récupérer l'annotation pour les paramètres
        EnableDynamicSecurity annotation = findEnableDynamicSecurityAnnotation();

        long refreshInterval = annotation != null ? annotation.refreshInterval() : 300000;
        long initialDelay = annotation != null ? annotation.initialDelay() : 10000;

        return new DynamicSecurityLoader(discoveryClient, webClientBuilder, refreshInterval, initialDelay);
    }

    @Bean
    public DynamicAuthorizationManager dynamicAuthorizationManager(DynamicSecurityLoader securityLoader) {
        log.info("🔧 Configuring DynamicAuthorizationManager");
        return new DynamicAuthorizationManager(securityLoader);
    }

    @Bean
    public SecurityRulesController securityRulesController(DynamicSecurityLoader securityLoader) {
        log.info("🔧 Configuring SecurityRulesController for manual reload");
        return new SecurityRulesController(securityLoader);
    }

    /**
     * Trouve l'annotation @EnableDynamicSecurity dans l'application
     */
    private EnableDynamicSecurity findEnableDynamicSecurityAnnotation() {
        try {
            // Chercher l'annotation dans la classe principale
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (element.getMethodName().equals("main")) {
                    Class<?> mainClass = Class.forName(element.getClassName());
                    EnableDynamicSecurity annotation = AnnotationUtils.findAnnotation(
                        mainClass, EnableDynamicSecurity.class);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not find @EnableDynamicSecurity annotation", e);
        }
        return null;
    }
}
