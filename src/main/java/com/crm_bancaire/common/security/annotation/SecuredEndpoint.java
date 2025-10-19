package com.crm_bancaire.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour sécuriser un endpoint avec des rôles spécifiques
 *
 * Exemple d'utilisation :
 * <pre>
 * @GetMapping("/users")
 * @SecuredEndpoint(roles = {"ADMIN", "SUPER_ADMIN"})
 * public List<UserResponse> getAllUsers() {
 *     return userService.getAllUsers();
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredEndpoint {
    /**
     * Rôles autorisés à accéder à cet endpoint
     * Utilisez les noms de rôles comme String (ex: "ADMIN", "SUPER_ADMIN", "CLIENT")
     */
    String[] roles();
}
