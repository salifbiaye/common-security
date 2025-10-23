# 👤 Guide UserContext - Common Security

Tout savoir sur UserContext pour accéder à l'utilisateur courant.

---

## 🎯 Qu'est-ce que UserContext?

`UserContext` est une classe `ThreadLocal` qui stocke les informations de l'utilisateur authentifié pour la requête courante.

**Avantages**:
- ✅ Accessible **partout** (controllers, services, repositories, etc.)
- ✅ **Thread-safe** (chaque requête HTTP a son propre contexte)
- ✅ **Automatiquement rempli** par `JwtUserInterceptor`
- ✅ **Automatiquement nettoyé** à la fin de la requête

---

## 🚀 Utilisation de base

### Activation

```java
@SpringBootApplication
@EnableUserContext
public class MyServiceApplication {}
```

### Récupérer l'utilisateur courant

```java
@Service
public class MyService {

    public void doSomething() {
        // Récupérer toutes les infos
        UserContext.ActorInfo actor = UserContext.getCurrentActor();

        String sub = actor.getSub();              // ID utilisateur (Keycloak sub)
        String email = actor.getEmail();          // email@example.com
        String username = actor.getUsername();    // john.doe
        String firstName = actor.getFirstName();  // John
        String lastName = actor.getLastName();    // Doe
        String role = actor.getRole();            // ADMIN, USER, etc.
    }

    public void quickAccess() {
        // Juste le sub
        String userSub = UserContext.getCurrentUserSub();
    }
}
```

---

## 💡 Cas d'usage

### 1. Audit automatique

```java
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditRepository;

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

### 2. Events avec information de l'acteur

```java
@Service
public class UserEventPublisher {

    public void publishUserCreated(User user) {
        UserContext.ActorInfo actor = UserContext.getCurrentActor();

        UserEvent event = UserEvent.builder()
            .eventType("USER_CREATED")
            .userId(user.getId())
            .userSub(user.getSub())
            .actorSub(actor.getSub())        // Qui a créé
            .actorEmail(actor.getEmail())
            .actorRole(actor.getRole())
            .timestamp(LocalDateTime.now())
            .build();

        streamBridge.send("user-events", event);
    }
}
```

### 3. Filtrage par utilisateur

```java
@Service
public class DocumentService {

    public List<Document> getMyDocuments() {
        String currentUserSub = UserContext.getCurrentUserSub();

        // Récupérer seulement les documents de l'utilisateur courant
        return documentRepository.findByOwnerSub(currentUserSub);
    }
}
```

### 4. Validation des permissions

```java
@Service
public class DocumentService {

    public void deleteDocument(String documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document not found"));

        UserContext.ActorInfo actor = UserContext.getCurrentActor();

        // Vérifier que c'est le propriétaire ou un admin
        if (!doc.getOwnerSub().equals(actor.getSub()) &&
            !actor.getRole().equals("ADMIN")) {
            throw new ForbiddenException("You can only delete your own documents");
        }

        documentRepository.delete(doc);
    }
}
```

---

## 🔧 Providers JWT personnalisés

### Keycloak avec client custom

```java
@Component
public class MyKeycloakExtractor implements JwtClaimExtractor {

    @Override
    public ActorInfo extractFromJwt(Jwt jwt) {
        return ActorInfo.builder()
            .sub(jwt.getClaimAsString("sub"))
            .email(jwt.getClaimAsString("email"))
            .username(jwt.getClaimAsString("preferred_username"))
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .role(extractRoleFromCustomClient(jwt, "my-client"))
            .build();
    }

    private String extractRoleFromCustomClient(Jwt jwt, String clientName) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        // ... extraction custom
    }
}
```

### Auth0

```java
@Component
public class Auth0Extractor implements JwtClaimExtractor {

    @Override
    public ActorInfo extractFromJwt(Jwt jwt) {
        return ActorInfo.builder()
            .sub(jwt.getClaimAsString("sub"))
            .email(jwt.getClaimAsString("email"))
            .username(jwt.getClaimAsString("nickname"))  // ← Auth0 utilise "nickname"
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .role(extractAuth0Role(jwt))
            .build();
    }

    private String extractAuth0Role(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("https://myapp.com/roles");
        return roles != null && !roles.isEmpty() ? roles.get(0) : "USER";
    }
}
```

---

## ⚠️ Bonnes pratiques

### ✅ DO

```java
// Dans un controller/service Spring
@Service
public class MyService {
    public void doSomething() {
        UserContext.ActorInfo actor = UserContext.getCurrentActor();
        // ✅ OK - Dans le contexte d'une requête HTTP
    }
}
```

### ❌ DON'T

```java
// Dans un @Scheduled ou un thread séparé
@Scheduled(fixedDelay = 5000)
public void scheduledTask() {
    UserContext.ActorInfo actor = UserContext.getCurrentActor();
    // ❌ PAS OK - Pas de requête HTTP = pas de contexte
    // actor sera NULL
}

// Dans un CompletableFuture
CompletableFuture.runAsync(() -> {
    UserContext.ActorInfo actor = UserContext.getCurrentActor();
    // ❌ PAS OK - Thread différent = pas de contexte
});
```

### Vérifier si l'utilisateur est authentifié

```java
UserContext.ActorInfo actor = UserContext.getCurrentActor();
if (actor == null) {
    // Pas d'utilisateur authentifié (endpoint public, scheduled task, etc.)
    log.warn("No authenticated user");
    return;
}

// Utiliser actor en toute sécurité
String email = actor.getEmail();
```

---

## 🔍 Troubleshooting

### UserContext.getCurrentActor() retourne null

**Causes possibles**:
1. L'endpoint n'est pas intercepté → Vérifiez `pathPatterns` dans `@EnableUserContext`
2. Pas de JWT dans la requête → Endpoint public ou requête non authentifiée
3. Thread différent → Vous êtes dans un @Scheduled ou CompletableFuture

### Les informations sont incorrectes

**Cause**: Le `JwtClaimExtractor` par défaut ne correspond pas à votre JWT.

**Solution**: Créez un `JwtClaimExtractor` custom (voir exemples ci-dessus).

---

[← Guide Gateway](GATEWAY_GUIDE.md) | [Exemples →](EXAMPLES.md)
