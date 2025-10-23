package com.crm_bancaire.common.security.httpclient.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration pour WebClient avec propagation automatique du JWT.
 * S'active si WebClient est présent dans le classpath.
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@Slf4j
public class WebClientAutoConfiguration {

    @Bean
    public WebClientAuthFilter webClientAuthFilter() {
        log.info("🔧 Configuring WebClientAuthFilter for automatic JWT propagation");
        return new WebClientAuthFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder(WebClientAuthFilter authFilter) {
        log.info("🔧 Creating default WebClient.Builder with JWT propagation");
        return WebClient.builder().filter(authFilter);
    }
}
