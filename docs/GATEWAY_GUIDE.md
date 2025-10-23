# 🌐 Guide Gateway - Common Security

Guide pour utiliser `common-security` dans Spring Cloud Gateway.

---

## 🎯 Fonctionnement

Le Gateway utilise **Eureka** pour découvrir automatiquement les microservices, appelle leur endpoint `/security/rules`, et applique les règles dynamiquement.

```
┌─────────────────┐
│     Eureka      │ ← Enregistrement des services
└────────┬────────┘
         │
    ┌────▼────────────────────────────┐
    │         Gateway                 │
    │  @EnableDynamicSecurity         │
    │                                 │
    │  1. Découvre services (Eureka)  │
    │  2. GET /security/rules         │
    │  3. Applique règles             │
    └────────┬────────────────────────┘
             │
        ┌────▼──────┐
        │  Service  │  ← @SecuredEndpoint / @PublicEndpoint
        └───────────┘
```

---

## 🚀 Installation

```xml
<dependency>
    <groupId>com.github.salifbiaye</groupId>
    <artifactId>common-security</artifactId>
    <version>v1.0.13</version>
</dependency>
```

---

## Configuration

### 1. Activer la sécurité dynamique

```java
@SpringBootApplication
@EnableDynamicSecurity
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

### 2. Simplifier SecurityConfig

Le Gateway va utiliser `DynamicAuthorizationManager` automatiquement:

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final DynamicAuthorizationManager dynamicAuthorizationManager;

    public SecurityConfig(DynamicAuthorizationManager dynamicAuthorizationManager) {
        this.dynamicAuthorizationManager = dynamicAuthorizationManager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(auth -> {
                // Règles publiques statiques
                auth.pathMatchers("/actuator/**", "/eureka/**").permitAll();
                auth.pathMatchers("/security/rules").permitAll();
                auth.pathMatchers("/admin/**").permitAll();

                // 🎯 Toutes les autres requêtes = autorisation dynamique
                auth.anyExchange().access(dynamicAuthorizationManager);
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())));

        return http.build();
    }

    // Votre JWT converter habituel
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        // ...
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // ...
    }
}
```

---

## ⚙️ Configuration avancée

### Personnaliser les paramètres

```java
@EnableDynamicSecurity(
    refreshInterval = 600000,  // Rechargement toutes les 10 minutes (default: 5 min)
    initialDelay = 15000        // Attendre 15 secondes au démarrage (default: 10 sec)
)
public class GatewayApplication {}
```

---

## 🔄 Rechargement manuel

### Endpoint admin

Le Gateway expose automatiquement `/admin/security/reload`:

```bash
GET http://localhost:8088/admin/security/reload
```

**Réponse**:
```json
{
  "message": "Security rules reloaded successfully",
  "services": ["sib-user-service", "sib-customer-service"],
  "totalRules": 25
}
```

---

## 📊 Logs

Au démarrage et à chaque refresh, vous verrez:

```
🔍 Discovering services from Eureka...
📋 Found services: [sib-user-service, sib-customer-service]
🔄 Attempting to load rules from service: sib-user-service
✅ Loaded 15 security rules from sib-user-service
🔄 Attempting to load rules from service: sib-customer-service
✅ Loaded 10 security rules from sib-customer-service
🎯 Security rules loading completed. Total services: 2
```

Lors d'une requête:
```
🔍 Checking authorization for GET /api/users
📋 Loaded rules from services: [sib-user-service]
✅ Found matching rule: GET /api/users → [ADMIN, SUPER_ADMIN]
👤 User roles: [CHARGE_DE_CLIENTELE]
❌ Access DENIED
```

---

## 🔧 Troubleshooting

### Aucune règle n'est chargée

**Logs**:
```
📋 Found services: []
```

**Causes possibles**:
- Eureka n'est pas démarré
- Les services ne sont pas enregistrés dans Eureka
- Le Gateway démarre avant les services

**Solution**: Attendez 10 secondes (initialDelay) ou appelez `/admin/security/reload`.

### Erreur "Invalid scheme [lb]"

**Cause**: `WebClient.Builder` sans `@LoadBalanced`.

**Solution**: `@EnableDynamicSecurity` configure automatiquement un `WebClient.Builder` avec `@LoadBalanced`. Supprimez votre bean manuel si vous en avez un.

### Les règles ne se mettent pas à jour

**Solution**: Appelez manuellement `/admin/security/reload` ou attendez le prochain refresh auto (5 minutes).

---

## 📝 Résumé

1. ✅ Ajouter `@EnableDynamicSecurity`
2. ✅ Injecter `DynamicAuthorizationManager` dans `SecurityConfig`
3. ✅ Utiliser `.access(dynamicAuthorizationManager)` pour les routes dynamiques
4. ✅ Les règles se rechargent automatiquement toutes les 5 minutes
5. ✅ Forcer le rechargement via `GET /admin/security/reload`

**C'est tout!** 🚀

---

[← Guide Microservice](MICROSERVICE_GUIDE.md) | [Guide UserContext →](USER_CONTEXT_GUIDE.md)
