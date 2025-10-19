package com.crm_bancaire.common.security.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listener résilient pour les événements de découverte/enregistrement.
 *
 * Nous n'importons pas des classes Spring Cloud spécifiques pour éviter
 * les problèmes de dépendances ; à la place on détecte les événements
 * par nom de classe et on déclenche un reload idempotent des règles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscoveryEventListener {

    private final DynamicSecurityLoader dynamicSecurityLoader;

    // Noms d'événements (class.getName()) à surveiller
    private static final Set<String> WATCHED_EVENT_NAMES = Set.of(
            "org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent",
            "org.springframework.cloud.client.serviceregistry.event.InstanceRegisteredEvent",
            "org.springframework.cloud.client.discovery.event.HeartbeatEvent",
            "org.springframework.cloud.client.serviceregistry.Registration",
            "org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent",
            "org.springframework.cloud.netflix.eureka.serviceloader.event.EurekaInstanceRegisteredEvent"
    );

    // Debounce: ne pas relancer plus d'une fois toutes les 5 secondes
    private final AtomicLong lastReloadEpochSec = new AtomicLong(0);

    @EventListener
    public void onAnyEvent(ApplicationEvent event) {
        String className = event.getClass().getName();

        if (WATCHED_EVENT_NAMES.contains(className)) {
            long now = Instant.now().getEpochSecond();
            long last = lastReloadEpochSec.get();
            if (now - last < 5) {
                log.debug("Discovery event {} received but debounced (last reload {}s ago)", className, now - last);
                return;
            }
            if (lastReloadEpochSec.compareAndSet(last, now)) {
                log.info("Discovery event {} detected — triggering security rules reload", className);
                try {
                    dynamicSecurityLoader.loadSecurityRules();
                } catch (Exception ex) {
                    log.error("Error while reloading security rules after discovery event", ex);
                }
            }
        } else {
            // debug logging optionally
            log.trace("Ignored event: {}", className);
        }
    }
}
