package com.crm_bancaire.common.security.httpclient.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration pour RestClient (Spring 6.1+) avec propagation automatique du JWT.
 * S'active si RestClient est présent dans le classpath.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.web.client.RestClient")
@Slf4j
public class RestClientAutoConfiguration {

    @Bean
    public RestClientAuthInterceptor restClientAuthInterceptor() {
        log.info("🔧 Configuring RestClientAuthInterceptor for automatic JWT propagation");
        return new RestClientAuthInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder(RestClientAuthInterceptor authInterceptor) {
        log.info("🔧 Creating default RestClient.Builder with JWT propagation");
        return RestClient.builder()
            .requestInterceptor(authInterceptor);
    }
}
