# 🔐 Common Security

**Librairie de sécurité réutilisable pour microservices Spring Boot**

Simplifiez la gestion de la sécurité dans vos microservices avec des annotations et une auto-configuration intelligente.

[![](https://jitpack.io/v/salifbiaye/common-security.svg)](https://jitpack.io/#salifbiaye/common-security)

---

## ✨ Fonctionnalités

### Pour les Microservices (Spring MVC)

- ✅ **@SecuredEndpoint** - Sécurisez vos endpoints avec des rôles
- ✅ **@PublicEndpoint** - Marquez les endpoints publics
- ✅ **@EnableUserContext** - Accédez facilement à l'utilisateur courant
- ✅ **UserContext.getCurrentActor()** - Infos JWT partout dans le code
- ✅ **Support Keycloak** par défaut (extensible)

### Pour le Gateway (Spring Cloud Gateway)

- ✅ **@EnableDynamicSecurity** - Sécurité dynamique avec Eureka
- ✅ **Découverte automatique** des règles depuis les services
- ✅ **Zéro duplication** - Chaque service gère ses autorisations
- ✅ **Rechargement auto** toutes les 5 minutes

---

## 🚀 Quick Start

### Installation

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.salifbiaye</groupId>
    <artifactId>common-security</artifactId>
    <version>v1.0.13</version>
</dependency>
```

### Microservice

```java
@SpringBootApplication
@EnableUserContext
public class UserServiceApplication {}

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    @SecuredEndpoint(roles = {"ADMIN", "SUPER_ADMIN"})
    public List<User> getAll() {
        String email = UserContext.getCurrentActor().getEmail();
        return userService.findAll();
    }
}
```

### Gateway

```java
@SpringBootApplication
@EnableDynamicSecurity
public class GatewayApplication {}
```

---

## 📖 Documentation complète

- **[Guide Microservice](docs/MICROSERVICE_GUIDE.md)** - Tout sur l'utilisation dans un service
- **[Guide Gateway](docs/GATEWAY_GUIDE.md)** - Configuration du Gateway
- **[Guide UserContext](docs/USER_CONTEXT_GUIDE.md)** - Utilisation avancée du UserContext
- **[Référence API](docs/API_REFERENCE.md)** - Toutes les annotations et classes
- **[Exemples](docs/EXAMPLES.md)** - Code complet pour différents cas

---

Made with ❤️ for Spring Boot Microservices
