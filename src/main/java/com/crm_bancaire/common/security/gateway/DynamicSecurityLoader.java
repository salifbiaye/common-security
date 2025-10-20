package com.crm_bancaire.common.security.gateway;

import com.crm_bancaire.common.security.dto.EndpointRule;
import com.crm_bancaire.common.security.dto.SecurityRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
    private ScheduledExecutorService scheduler;

    public DynamicSecurityLoader(DiscoveryClient discoveryClient, WebClient.Builder webClientBuilder,
                                 long refreshInterval, long initialDelay) {
        this.discoveryClient = discoveryClient;
        this.webClientBuilder = webClientBuilder;
        this.refreshInterval = refreshInterval;
        this.initialDelay = initialDelay;

        log.info("🎯 DynamicSecurityLoader initialized with:");
        log.info("   ⏱️  Initial Delay: {} ms ({} seconds)", initialDelay, initialDelay / 1000.0);
        log.info("   🔄 Refresh Interval: {} ms ({} seconds)", refreshInterval, refreshInterval / 1000.0);
    }

    @PostConstruct
    public void scheduleSecurityRulesLoading() {
        log.info("🚀 Starting security rules scheduler...");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SecurityRulesLoader");
            thread.setDaemon(true);
            return thread;
        });

        // Premier chargement après initialDelay, puis répétition toutes les refreshInterval
        scheduler.scheduleWithFixedDelay(
                () -> {
                    try {
                        log.info("⏰ [SCHEDULED EXECUTION] Starting security rules refresh...");
                        loadSecurityRules();
                    } catch (Exception e) {
                        log.error("❌ Error during scheduled security rules loading", e);
                    }
                },
                initialDelay,           // Délai avant le premier lancement
                refreshInterval,        // Intervalle entre chaque exécution
                TimeUnit.MILLISECONDS
        );

        log.info("✅ Scheduler configured successfully");
        log.info("   → First execution in {} seconds", initialDelay / 1000.0);
        log.info("   → Then every {} seconds", refreshInterval / 1000.0);
    }

    @PreDestroy
    public void shutdown() {
        log.info("🛑 Shutting down security rules scheduler...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("⚠️ Scheduler did not terminate in time, forcing shutdown");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("❌ Interrupted while waiting for scheduler shutdown");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("✅ Scheduler shutdown completed");
    }

    public void loadSecurityRules() {
        log.info("🔍 Discovering services from Eureka...");

        List<String> services = discoveryClient.getServices();
        log.info("📋 Found {} service(s): {}", services.size(), services);

        if (services.isEmpty()) {
            log.warn("⚠️ No services found in Eureka registry");
            return;
        }

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

                                }
                            })
                            .onErrorResume(e -> {
                                log.warn("⚠️ Could not load security rules from {}: {} ({})",
                                        serviceName, e.getMessage(), e.getClass().getSimpleName());
                                return Mono.empty();
                            });
                }, 8)
                .collectList()
                .doOnSuccess(list -> {
                    log.info("🎯 Security rules loading completed");
                    log.info("   → Total services with rules: {}", securityRules.size());
                    log.info("   → Next refresh in {} seconds", refreshInterval / 1000.0);
                })
                .doOnError(e -> log.error("❌ Error during security rules loading", e))
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