package com.crm_bancaire.common.security.context;

import com.crm_bancaire.common.security.interceptor.JwtUserInterceptor;
import com.crm_bancaire.common.security.jwt.JwtClaimExtractor;
import com.crm_bancaire.common.security.jwt.KeycloakJwtClaimExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration pour activer le UserContext dans les microservices.
 *
 * Cette configuration est automatiquement chargée lorsque @EnableUserContext est utilisé.
 *
 * Elle configure:
 * - {@link JwtUserInterceptor} pour extraire le JWT et remplir le UserContext
 * - Enregistrement de l'interceptor sur les path patterns spécifiés
 *
 * Condition: Seulement pour les applications Spring MVC (pas WebFlux)
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Slf4j
public class UserContextAutoConfiguration {

    /**
     * Bean JwtClaimExtractor par défaut (Keycloak).
     * Si l'utilisateur crée son propre @Component JwtClaimExtractor, celui-ci sera ignoré.
     */
    @Bean
    @ConditionalOnMissingBean(JwtClaimExtractor.class)
    public JwtClaimExtractor jwtClaimExtractor() {
        log.info("🔧 Using default KeycloakJwtClaimExtractor");
        return new KeycloakJwtClaimExtractor();
    }

    @Bean
    public JwtUserInterceptor jwtUserInterceptor(JwtClaimExtractor jwtClaimExtractor) {
        log.info("🔧 Configuring JwtUserInterceptor for UserContext");
        return new JwtUserInterceptor(jwtClaimExtractor);
    }

    @Bean
    public WebMvcConfigurer userContextWebMvcConfigurer(JwtUserInterceptor jwtUserInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // Récupérer les path patterns depuis l'annotation @EnableUserContext
                String[] pathPatterns = getPathPatternsFromAnnotation();

                log.info("🔧 Registering JwtUserInterceptor on paths: {}", String.join(", ", pathPatterns));

                registry.addInterceptor(jwtUserInterceptor)
                    .addPathPatterns(pathPatterns);
            }
        };
    }

    /**
     * Récupère les path patterns depuis l'annotation @EnableUserContext
     */
    private String[] getPathPatternsFromAnnotation() {
        try {
            // Chercher l'annotation dans la classe principale de l'application
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (element.getMethodName().equals("main")) {
                    Class<?> mainClass = Class.forName(element.getClassName());
                    EnableUserContext annotation = AnnotationUtils.findAnnotation(
                        mainClass, EnableUserContext.class);
                    if (annotation != null) {
                        return annotation.pathPatterns();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not find @EnableUserContext annotation, using defaults", e);
        }

        // Valeurs par défaut si l'annotation n'est pas trouvée
        return new String[]{"/api/**"};
    }
}
