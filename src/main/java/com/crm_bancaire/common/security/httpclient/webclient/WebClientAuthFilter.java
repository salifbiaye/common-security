package com.crm_bancaire.common.security.httpclient.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ExchangeFilterFunction pour WebClient afin de propager automatiquement le JWT.
 *
 * Lorsqu'un microservice fait un appel WebClient vers un autre microservice,
 * ce filter copie automatiquement le header "Authorization: Bearer ..."
 * de la requête HTTP entrante vers la requête WebClient sortante.
 *
 * Activation automatique via WebClientAutoConfiguration.
 */
@Slf4j
public class WebClientAuthFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String token = extractTokenFromRequest();

        if (token != null) {
            ClientRequest filteredRequest = ClientRequest.from(request)
                .header("Authorization", "Bearer " + token)
                .build();

            log.debug("🔐 Propagating JWT to WebClient call: {} {}", request.method(), request.url());
            return next.exchange(filteredRequest);
        }

        return next.exchange(request);
    }

    private String extractTokenFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract token from request context: {}", e.getMessage());
        }
        return null;
    }
}
