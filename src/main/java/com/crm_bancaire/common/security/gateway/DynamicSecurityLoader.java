package com.crm_bancaire.common.security.gateway;

import com.crm_bancaire.common.security.dto.EndpointRule;
import com.crm_bancaire.common.security.dto.SecurityRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Charge dynamiquement les règles de sécurité depuis tous les microservices enregistrés dans Eureka
 */
@Slf4j
public class DynamicSecurityLoader {

    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;
    private final long refreshInterval;
    private final long initialDelay;

    private final Map<String, List<EndpointRule>> securityRules = new ConcurrentHashMap<>();

    public DynamicSecurityLoader(DiscoveryClient discoveryClient, WebClient.Builder webClientBuilder,
                                  long refreshInterval, long initialDelay) {
        this.discoveryClient = discoveryClient;
        this.webClientBuilder = webClientBuilder;
        this.refreshInterval = refreshInterval;
        this.initialDelay = initialDelay;
    }

    @PostConstruct
    public void scheduleInitialLoad() {
        // Attendre après le démarrage pour que les services s'enregistrent dans Eureka
        log.info("⏰ Scheduling initial security rules load in {} ms...", initialDelay);
        new Thread(() -> {
            try {
                Thread.sleep(initialDelay);
                loadSecurityRules();
            } catch (InterruptedException e) {
                log.error("❌ Initial load interrupted", e);
            }
        }).start();
    }

    @Scheduled(fixedDelayString = "#{@dynamicSecurityLoader.refreshInterval}")
    public void loadSecurityRules() {
        log.info("🔍 Discovering services from Eureka...");

        // 1. Récupère la liste des services depuis Eureka
        List<String> services = discoveryClient.getServices();
        log.info("📋 Found services: {}", services);

        // Build a reactive pipeline: for each service, call /security/rules and update the map.
        Flux.fromIterable(services)
            .filter(serviceName -> {
                if (serviceName.equalsIgnoreCase("gateway") ||
                    serviceName.equalsIgnoreCase("sib-gateway-service") ||
                    serviceName.equalsIgnoreCase("sib-registry")) {
                    log.debug("⏭️ Skipping service: {}", serviceName);
                    return false;
                }
                return true;
            })
            .doOnNext(serviceName -> log.info("🔄 Attempting to load rules from service: {}", serviceName))
            .flatMap(serviceName -> {
                String uri = "lb://" + serviceName + "/security/rules";
                log.debug("   → Calling URI: {}", uri);

                return webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(SecurityRules.class)
                    .timeout(Duration.ofSeconds(5))
                    .doOnNext(rules -> {
                        if (rules != null && rules.getEndpoints() != null) {
                            securityRules.put(serviceName, rules.getEndpoints());
                            log.info("✅ Loaded {} security rules from {}",
                                     rules.getEndpoints().size(), serviceName);

                            rules.getEndpoints().forEach(rule ->
                                log.debug("   → {} {} = roles: {}, public: {}",
                                          rule.getMethods(), rule.getFullPath(),
                                          rule.getRoles(), rule.isPublic())
                            );
                        }
                    })
                    .onErrorResume(e -> {
                        log.warn("⚠️ Could not load security rules from {}: {} ({})",
                                 serviceName, e.getMessage(), e.getClass().getSimpleName());
                        return Mono.empty();
                    });
            }, 8) // concurrency 8
            .collectList()
            .doOnSuccess(list -> log.info("🎯 Security rules loading completed. Total services: {}", securityRules.size()))
            .subscribe();
    }

    /**
     * Retourne toutes les règles de sécurité chargées
     */
    public Map<String, List<EndpointRule>> getAllRules() {
        return securityRules;
    }

    /**
     * Retourne les règles pour un service spécifique
     */
    public List<EndpointRule> getRulesForService(String serviceName) {
        return securityRules.get(serviceName);
    }

    public long getRefreshInterval() {
        return refreshInterval;
    }

    public long getInitialDelay() {
        return initialDelay;
    }
}
