package com.crm_bancaire.common.security.httpclient.resttemplate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration pour RestTemplate avec propagation automatique du JWT.
 * S'active si RestTemplate est présent dans le classpath.
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@Slf4j
public class RestTemplateAutoConfiguration {

    @Bean
    public RestTemplateAuthInterceptor restTemplateAuthInterceptor() {
        log.info("🔧 Configuring RestTemplateAuthInterceptor for automatic JWT propagation");
        return new RestTemplateAuthInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder(RestTemplateAuthInterceptor authInterceptor) {
        log.info("🔧 Creating default RestTemplateBuilder with JWT propagation");
        return new RestTemplateBuilder()
            .additionalInterceptors(authInterceptor);
    }
}
