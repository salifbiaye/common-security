package com.crm_bancaire.common.security.scanner;

import com.crm_bancaire.common.security.annotation.PublicEndpoint;
import com.crm_bancaire.common.security.annotation.SecuredEndpoint;
import com.crm_bancaire.common.security.dto.EndpointRule;
import com.crm_bancaire.common.security.dto.SecurityRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scanner qui détecte automatiquement les annotations de sécurité sur les controllers
 * et génère les règles de sécurité correspondantes
 */
@Component
@Slf4j
public class SecurityRulesScanner {

    private final ApplicationContext applicationContext;

    @Value("${spring.application.name}")
    private String serviceName;

    private SecurityRules securityRules;

    public SecurityRulesScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scanControllers() {
        log.info("🔍 Scanning controllers for security annotations in service: {}", serviceName);

        List<EndpointRule> allRules = new ArrayList<>();

        // Récupère tous les beans annotés avec @RestController
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);

        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
            String beanName = entry.getKey();
            Object controller = entry.getValue();
            Class<?> controllerClass = controller.getClass();

            // Skip SecurityMetadataController pour éviter la dépendance circulaire
            if (beanName.contains("SecurityMetadataController") ||
                beanName.contains("securityMetadataController")) {
                log.debug("⏭️ Skipping SecurityMetadataController (internal security endpoint)");
                continue;
            }

            // Récupère le @RequestMapping au niveau de la classe
            RequestMapping classMapping = controllerClass.getAnnotation(RequestMapping.class);
            if (classMapping == null) {
                continue;
            }

            String basePath = classMapping.value().length > 0 ? classMapping.value()[0] : "";

            // Scanne toutes les méthodes du controller
            for (Method method : controllerClass.getDeclaredMethods()) {
                EndpointRule rule = extractRuleFromMethod(method, basePath);
                if (rule != null) {
                    allRules.add(rule);
                    log.debug("✅ Found endpoint: {} {} → roles: {}, public: {}",
                            rule.getMethods(), rule.getFullPath(), rule.getRoles(), rule.isPublic());
                }
            }
        }

        this.securityRules = SecurityRules.builder()
                .serviceName(serviceName)
                .basePath("")
                .endpoints(allRules)
                .build();

        log.info("✅ Security rules scanning completed. Found {} endpoints", allRules.size());
    }

    private EndpointRule extractRuleFromMethod(Method method, String basePath) {
        // Vérifie si la méthode a une annotation de sécurité
        SecuredEndpoint securedEndpoint = method.getAnnotation(SecuredEndpoint.class);
        PublicEndpoint publicEndpoint = method.getAnnotation(PublicEndpoint.class);

        if (securedEndpoint == null && publicEndpoint == null) {
            return null; // Pas d'annotation de sécurité
        }

        // Récupère le chemin et les méthodes HTTP
        String path = "";
        List<String> httpMethods = new ArrayList<>();

        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            path = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethods.add("GET");
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            path = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethods.add("POST");
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            path = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethods.add("PUT");
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            path = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethods.add("DELETE");
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            path = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethods.add("PATCH");
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            path = mapping.value().length > 0 ? mapping.value()[0] : "";
            if (mapping.method().length > 0) {
                httpMethods.addAll(Arrays.stream(mapping.method())
                        .map(Enum::name)
                        .collect(Collectors.toList()));
            }
        }

        if (httpMethods.isEmpty()) {
            return null;
        }

        // Construit la règle
        EndpointRule.EndpointRuleBuilder builder = EndpointRule.builder()
                .basePath(basePath)
                .path(path)
                .methods(httpMethods);

        if (publicEndpoint != null) {
            builder.isPublic(true).roles(Collections.emptyList());
        } else if (securedEndpoint != null) {
            List<String> roles = Arrays.asList(securedEndpoint.roles());
            builder.isPublic(false).roles(roles);
        }

        return builder.build();
    }

    public SecurityRules getSecurityRules() {
        return securityRules;
    }
}
