package com.crm_bancaire.common.security.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interceptor Feign pour propager automatiquement le JWT Authorization header.
 *
 * Lorsqu'un microservice fait un appel Feign vers un autre microservice,
 * cet interceptor copie automatiquement le header "Authorization: Bearer ..."
 * de la requête HTTP entrante vers la requête Feign sortante.
 *
 * Cela permet de maintenir le contexte d'authentification à travers les appels inter-services.
 *
 * Activation automatique:
 * - Si Feign est présent dans le classpath
 * - Via FeignAutoConfiguration
 */
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                // Propager l'Authorization header vers le service appelé
                requestTemplate.header("Authorization", authorizationHeader);
                log.debug("🔐 Propagating JWT to Feign call: {} {}",
                    requestTemplate.method(), requestTemplate.url());
            }
        }
    }
}
