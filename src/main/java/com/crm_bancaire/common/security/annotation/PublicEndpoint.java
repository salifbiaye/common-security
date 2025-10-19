package com.crm_bancaire.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer un endpoint comme public (pas d'authentification requise)
 *
 * Exemple d'utilisation :
 * <pre>
 * @PostMapping("/register")
 * @PublicEndpoint
 * public ResponseEntity<String> register(@RequestBody CustomerRequest request) {
 *     return customerService.register(request);
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
}
