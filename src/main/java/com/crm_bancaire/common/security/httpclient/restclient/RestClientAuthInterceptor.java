package com.crm_bancaire.common.security.httpclient.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Interceptor pour RestClient (Spring 6.1+) afin de propager automatiquement le JWT.
 *
 * Lorsqu'un microservice fait un appel RestClient vers un autre microservice,
 * cet interceptor copie automatiquement le header "Authorization: Bearer ..."
 * de la requête HTTP entrante vers la requête RestClient sortante.
 *
 * Activation automatique via RestClientAutoConfiguration.
 */
@Slf4j
public class RestClientAuthInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String token = extractTokenFromRequest();

        if (token != null && !token.isEmpty()) {
            request.getHeaders().set("Authorization", "Bearer " + token);
            log.debug("🔐 Propagating JWT to RestClient call: {} {}", request.getMethod(), request.getURI());
        }

        return execution.execute(request, body);
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
