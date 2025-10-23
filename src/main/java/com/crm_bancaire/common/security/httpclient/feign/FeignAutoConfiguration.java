package com.crm_bancaire.common.security.httpclient.feign;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration pour Feign avec propagation automatique du JWT.
 *
 * Cette configuration s'active automatiquement SI:
 * - Feign est présent dans le classpath (spring-cloud-starter-openfeign)
 *
 * Elle configure:
 * - {@link FeignAuthInterceptor} pour propager le JWT Authorization header
 *
 * Les microservices n'ont rien à faire - la propagation JWT est automatique.
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
@Slf4j
public class FeignAutoConfiguration {

    @Bean
    public FeignAuthInterceptor feignAuthInterceptor() {
        log.info("🔧 Configuring FeignAuthInterceptor for automatic JWT propagation");
        return new FeignAuthInterceptor();
    }
}
