# 📘 Guide Microservice - Common Security

Guide complet pour utiliser `common-security` dans un microservice Spring MVC.

---

## ⚡ Configuration par défaut (à savoir!)

| Configuration | Valeur par défaut | Comment changer |
|--------------|-------------------|-----------------|
| **Paths interceptés** | `/api/**` seulement | `@EnableUserContext(pathPatterns = {...})` |
| **Provider JWT** | Keycloak | Créer un `@Component JwtClaimExtractor` |
| **Client Keycloak** | `oauth2-pkce` | Créer un `@Component JwtClaimExtractor` |
| **Rôle par défaut** | `USER` | Dans votre `JwtClaimExtractor` custom |

⚠️ **Si votre client Keycloak n'est PAS `oauth2-pkce`** → Voir [Cas 1: Keycloak avec CLIENT ID différent](#cas-1-keycloak-avec-un-client-id-différent)

---

## 📋 Table des matières

1. [Installation](#installation)
2. [Configuration de base](#configuration-de-base)
3. [Sécuriser les endpoints](#sécuriser-les-endpoints)
4. [Utiliser UserContext](#utiliser-usercontext)
5. [Providers JWT personnalisés](#providers-jwt-personnalisés)
6. [Troubleshooting](#troubleshooting)

---

## Installation

### 1. Ajouter le repository JitPack

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Ajouter la dépendance

```xml
<dependency>
    <groupId>com.github.salifbiaye</groupId>
    <artifactId>common-security</artifactId>
    <version>v1.0.13</version>
</dependency>
```

---

## Configuration de base

### Activer UserContext

Ajoutez `@EnableUserContext` sur votre classe principale:

```java
package com.example.myservice;

import com.crm_bancaire.common.security.context.EnableUserContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableUserContext
public class MyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyServiceApplication.class, args);
    }
}
```

**C'est tout!** 🎉 Avec Keycloak, aucune autre configuration n'est nécessaire.

### Configuration des paths interceptés

⚠️ **IMPORTANT**: Par défaut, `@EnableUserContext` intercepte **UNIQUEMENT `/api/**`**

Si vos endpoints utilisent d'autres chemins, vous DEVEZ les spécifier:

```java
// ❌ Par défaut - Intercepte SEULEMENT /api/**
@EnableUserContext
public class MyServiceApplication {}

// ✅ Custom - Intercepte /api/**, /internal/**, /public/**
@EnableUserContext(pathPatterns = {"/api/**", "/internal/**", "/public/**"})
public class MyServiceApplication {}

// ✅ Tout intercepter (déconseillé - overhead sur tous les endpoints)
@EnableUserContext(pathPatterns = {"/**"})
public class MyServiceApplication {}
```

**Exemple concret**:

```java
// Si vos controllers sont comme ça:
@RequestMapping("/api/users")       // ✅ Intercepté par défaut
@RequestMapping("/internal/health") // ❌ PAS intercepté par défaut
@RequestMapping("/public/stats")    // ❌ PAS intercepté par défaut

// Il faut faire:
@EnableUserContext(pathPatterns = {"/api/**", "/internal/**", "/public/**"})
```

---

## Sécuriser les endpoints

### `@SecuredEndpoint` - Endpoints protégés

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    @SecuredEndpoint(roles = {"ADMIN", "SUPER_ADMIN"})
    public List<User> getAllUsers() {
        // Seuls ADMIN et SUPER_ADMIN peuvent accéder
        return userService.findAll();
    }

    @PostMapping
    @SecuredEndpoint(roles = {"SUPER_ADMIN"})
    public User createUser(@RequestBody UserRequest request) {
        // Seul SUPER_ADMIN peut créer
        return userService.create(request);
    }

    @GetMapping("/{id}")
    @SecuredEndpoint(roles = {"ADMIN", "SUPER_ADMIN", "CHARGE_DE_CLIENTELE"})
    public User getUserById(@PathVariable String id) {
        // 3 rôles autorisés
        return userService.findById(id);
    }
}
```

### `@PublicEndpoint` - Endpoints publics

```java
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/stats")
    @PublicEndpoint
    public Stats getPublicStats() {
        // Accessible sans authentification
        return statsService.getPublicStats();
    }

    @PostMapping("/register")
    @PublicEndpoint
    public void register(@RequestBody RegisterRequest request) {
        // Inscription publique
        userService.register(request);
    }
}
```

### Pas d'annotation = Pas de règle exposée

```java
@GetMapping("/internal")
public String internalEndpoint() {
    // Cet endpoint n'est PAS exposé au Gateway
    // Il reste accessible en direct (ex: appels inter-services)
    return "Internal use only";
}
```

---

## Utiliser UserContext

### Dans un Controller

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @GetMapping("/me")
    public ProfileResponse getMyProfile() {
        UserContext.ActorInfo actor = UserContext.getCurrentActor();

        return ProfileResponse.builder()
            .sub(actor.getSub())
            .email(actor.getEmail())
            .username(actor.getUsername())
            .firstName(actor.getFirstName())
            .lastName(actor.getLastName())
            .role(actor.getRole())
            .build();
    }
}
```

### Dans un Service

```java
@Service
public class AuditService {

    public void logAction(String action, String details) {
        UserContext.ActorInfo actor = UserContext.getCurrentActor();

        AuditLog log = AuditLog.builder()
            .actorSub(actor.getSub())
            .actorEmail(actor.getEmail())
            .actorRole(actor.getRole())
            .action(action)
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();

        auditRepository.save(log);
    }
}
```

### Dans un Event Publisher

```java
@Service
public class UserEventPublisher {

    private final StreamBridge streamBridge;

    public void publishUserCreated(User user) {
        UserContext.ActorInfo actor = UserContext.getCurrentActor();

        UserEvent event = UserEvent.builder()
            .eventType("USER_CREATED")
            .userId(user.getId())
            .actorSub(actor.getSub())
            .actorEmail(actor.getEmail())
            .timestamp(LocalDateTime.now())
            .build();

        streamBridge.send("user-events", event);
    }
}
```

### Méthodes disponibles

```java
// Récupérer toutes les infos
UserContext.ActorInfo actor = UserContext.getCurrentActor();

// Juste le sub (ID utilisateur)
String userSub = UserContext.getCurrentUserSub();

// Vérifier si un utilisateur est authentifié
if (UserContext.getCurrentActor() != null) {
    // Utilisateur authentifié
}
```

---

## Providers JWT personnalisés

### 🎯 Quand avez-vous besoin d'un extractor custom?

**Par défaut**, `common-security` utilise `KeycloakJwtClaimExtractor` qui:
- ✅ Extrait les claims standards: `sub`, `email`, `preferred_username`, `given_name`, `family_name`
- ✅ Extrait le rôle depuis: `resource_access.oauth2-pkce.roles[0]`

**Vous avez besoin d'un extractor custom SI**:
1. ❌ Votre client Keycloak n'est **PAS** nommé `oauth2-pkce`
2. ❌ Vous utilisez **Auth0, Okta, ou un autre provider OAuth2**
3. ❌ Vous avez un **JWT complètement custom**
4. ❌ Les claims sont dans des **champs différents**

### Cas 1: Keycloak avec un CLIENT ID DIFFÉRENT

**Problème**: Votre client Keycloak s'appelle `my-app` au lieu de `oauth2-pkce`.

**JWT Keycloak**:
```json
{
  "sub": "123-456",
  "email": "john@example.com",
  "resource_access": {
    "my-app": {              // ← Votre client s'appelle "my-app"
      "roles": ["ADMIN"]
    }
  }
}
```

**Solution**: Créez un extractor qui cible votre client:

```java
package com.example.myservice.config;

import com.crm_bancaire.common.security.context.UserContext.ActorInfo;
import com.crm_bancaire.common.security.jwt.JwtClaimExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component  // ← Spring va automatiquement utiliser CELUI-CI au lieu du défaut
public class MyAppJwtExtractor implements JwtClaimExtractor {

    private static final String MY_CLIENT_NAME = "my-app";  // ← VOTRE client ID
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ActorInfo extractFromJwt(Jwt jwt) {
        return ActorInfo.builder()
            .sub(jwt.getClaimAsString("sub"))
            .email(jwt.getClaimAsString("email"))
            .username(jwt.getClaimAsString("preferred_username"))
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .role(extractRoleFromKeycloak(jwt))  // ← Extraction avec votre client
            .build();
    }

    private String extractRoleFromKeycloak(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> myClient = (Map) resourceAccess.get(MY_CLIENT_NAME);
                if (myClient != null) {
                    List<String> roles = (List) myClient.get("roles");
                    if (roles != null && !roles.isEmpty()) {
                        return roles.get(0);  // Premier rôle
                    }
                }
            }
        } catch (Exception e) {
            // Log error
        }
        return "USER";  // Rôle par défaut
    }

    @Override
    public ActorInfo extractFromToken(String token) {
        try {
            // Décoder le JWT manuellement (pour les appels Feign)
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(decodedBytes));

            return ActorInfo.builder()
                .sub(getClaimAsString(payload, "sub"))
                .email(getClaimAsString(payload, "email"))
                .username(getClaimAsString(payload, "preferred_username"))
                .firstName(getClaimAsString(payload, "given_name"))
                .lastName(getClaimAsString(payload, "family_name"))
                .role(extractRoleFromNode(payload))
                .build();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRoleFromNode(JsonNode payload) {
        try {
            JsonNode resourceAccess = payload.get("resource_access");
            if (resourceAccess != null) {
                JsonNode myClient = resourceAccess.get(MY_CLIENT_NAME);
                if (myClient != null) {
                    JsonNode roles = myClient.get("roles");
                    if (roles != null && roles.isArray() && roles.size() > 0) {
                        return roles.get(0).asText();
                    }
                }
            }
        } catch (Exception e) {
            // Log error
        }
        return "USER";
    }

    private String getClaimAsString(JsonNode node, String claim) {
        JsonNode claimNode = node.get(claim);
        return claimNode != null ? claimNode.asText() : null;
    }
}
```

**C'est tout!** Spring va automatiquement utiliser `MyAppJwtExtractor` au lieu de `KeycloakJwtClaimExtractor`.

### Cas 2: Auth0

```java
@Component
public class Auth0JwtExtractor implements JwtClaimExtractor {

    @Override
    public ActorInfo extractFromJwt(Jwt jwt) {
        return ActorInfo.builder()
            .sub(jwt.getClaimAsString("sub"))
            .email(jwt.getClaimAsString("email"))
            .username(jwt.getClaimAsString("nickname"))  // Auth0 utilise "nickname"
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .role(extractAuth0Role(jwt))
            .build();
    }

    private String extractAuth0Role(Jwt jwt) {
        // Auth0 met les rôles dans un namespace custom
        List<String> roles = jwt.getClaimAsStringList("https://myapp.com/roles");
        return roles != null && !roles.isEmpty() ? roles.get(0) : "USER";
    }
}
```

### Cas 3: JWT complètement custom

```java
@Component
public class CustomJwtExtractor implements JwtClaimExtractor {

    @Override
    public ActorInfo extractFromJwt(Jwt jwt) {
        return ActorInfo.builder()
            .sub(jwt.getClaimAsString("userId"))        // Claim custom
            .email(jwt.getClaimAsString("mail"))        // Claim custom
            .username(jwt.getClaimAsString("login"))    // Claim custom
            .role(jwt.getClaimAsString("userRole"))     // Claim custom
            .build();
    }
}
```

---

## Troubleshooting

### UserContext est toujours null

**Cause**: L'interceptor ne s'exécute pas sur votre endpoint.

**Solution**: Vérifiez les path patterns:

```java
@EnableUserContext(pathPatterns = {"/api/**"})
```

Assurez-vous que votre endpoint correspond au pattern.

### Erreur "JwtClaimExtractor bean not found"

**Cause**: Aucun extractor n'est configuré.

**Solution**: L'extractor par défaut (Keycloak) devrait être auto-détecté. Vérifiez votre configuration Spring Boot.

### Les rôles ne sont pas extraits correctement

**Cause**: La structure JWT ne correspond pas à Keycloak standard.

**Solution**: Créez un `JwtClaimExtractor` custom (voir exemples ci-dessus).

### L'endpoint `/security/rules` retourne vide

**Cause**: Aucun endpoint n'a `@SecuredEndpoint` ou `@PublicEndpoint`.

**Solution**: Ajoutez les annotations sur vos controllers.

---

## 📝 Résumé

1. ✅ Ajouter la dépendance
2. ✅ Ajouter `@EnableUserContext` sur la classe principale
3. ✅ Utiliser `@SecuredEndpoint` et `@PublicEndpoint` sur les controllers
4. ✅ Utiliser `UserContext.getCurrentActor()` partout où vous en avez besoin
5. ✅ (Optionnel) Créer un `JwtClaimExtractor` custom pour d'autres providers

**C'est tout!** 🚀

---

[← Retour au README](../README.md) | [Guide Gateway →](GATEWAY_GUIDE.md)
