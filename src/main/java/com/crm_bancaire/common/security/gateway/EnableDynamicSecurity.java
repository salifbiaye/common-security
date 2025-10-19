package com.crm_bancaire.common.security.gateway;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation pour activer la sécurité dynamique dans le Gateway.
 *
 * Cette annotation active automatiquement:
 * - Le chargement dynamique des règles de sécurité depuis les microservices
 * - L'AuthorizationManager qui applique les règles dynamiquement
 * - Le WebClient avec LoadBalancer pour appeler les microservices
 *
 * Usage:
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableDynamicSecurity
 * public class GatewayApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(GatewayApplication.class, args);
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DynamicSecurityAutoConfiguration.class)
public @interface EnableDynamicSecurity {

    /**
     * Chemins à exempter de l'autorisation dynamique (toujours permit all)
     * Par défaut: /actuator/**, /eureka/**, /security/rules, /admin/**
     */
    String[] publicPaths() default {"/actuator/**", "/eureka/**", "/security/rules", "/admin/**"};

    /**
     * Interval de rafraîchissement des règles en millisecondes
     * Par défaut: 300000 (5 minutes)
     */
    long refreshInterval() default 300000;

    /**
     * Délai initial avant le premier chargement des règles en millisecondes
     * Par défaut: 10000 (10 secondes)
     */
    long initialDelay() default 10000;
}
