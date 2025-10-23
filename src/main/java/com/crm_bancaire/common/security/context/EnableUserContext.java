package com.crm_bancaire.common.security.context;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation pour activer automatiquement le UserContext dans un microservice.
 *
 * Cette annotation configure automatiquement:
 * - {@link UserContext} pour accéder à l'utilisateur courant
 * - {@link com.crm_bancaire.common.security.interceptor.JwtUserInterceptor} pour extraire le JWT
 * - {@link com.crm_bancaire.common.security.jwt.JwtClaimExtractor} pour parser les claims
 *
 * Usage minimal (Keycloak):
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableUserContext
 * public class MyServiceApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyServiceApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * Ensuite, utilisez UserContext partout dans votre code:
 * <pre>
 * &#64;Service
 * public class MyService {
 *     public void doSomething() {
 *         ActorInfo currentUser = UserContext.getCurrentActor();
 *         String userEmail = currentUser.getEmail();
 *         String userRole = currentUser.getRole();
 *     }
 * }
 * </pre>
 *
 * Configuration avancée:
 * <pre>
 * &#64;EnableUserContext(pathPatterns = {"/api/**", "/internal/**"})
 * </pre>
 *
 * Pour un provider OAuth2 différent de Keycloak:
 * - Créez un @Component qui implémente {@link com.crm_bancaire.common.security.jwt.JwtClaimExtractor}
 * - Il remplacera automatiquement l'implémentation par défaut
 *
 * @see UserContext
 * @see com.crm_bancaire.common.security.interceptor.JwtUserInterceptor
 * @see com.crm_bancaire.common.security.jwt.JwtClaimExtractor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(UserContextAutoConfiguration.class)
public @interface EnableUserContext {

    /**
     * Patterns de chemins à intercepter pour extraire le JWT.
     * Par défaut, intercepte tous les endpoints commençant par /api/
     *
     * @return Les patterns de chemins (Ant-style)
     */
    String[] pathPatterns() default {"/api/**"};
}
