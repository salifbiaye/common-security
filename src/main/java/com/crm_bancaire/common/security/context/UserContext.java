package com.crm_bancaire.common.security.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thread-local context pour stocker les informations de l'utilisateur courant.
 *
 * Utilisé pour accéder facilement aux informations de l'utilisateur authentifié
 * depuis n'importe où dans le code, sans avoir à passer le JWT partout.
 *
 * Usage:
 * <pre>
 * // Dans un controller, service, etc.
 * ActorInfo currentUser = UserContext.getCurrentActor();
 * String userEmail = currentUser.getEmail();
 * String userRole = currentUser.getRole();
 * </pre>
 *
 * Le contexte est automatiquement rempli par {@link com.crm_bancaire.common.security.interceptor.JwtUserInterceptor}
 * et nettoyé à la fin de chaque requête.
 */
public class UserContext {

    private static final ThreadLocal<ActorInfo> currentActor = new ThreadLocal<>();

    /**
     * Définit l'utilisateur courant pour le thread actuel
     */
    public static void setCurrentActor(ActorInfo actor) {
        currentActor.set(actor);
    }

    /**
     * Récupère les informations de l'utilisateur courant
     * @return Les informations de l'utilisateur, ou null si aucun utilisateur n'est authentifié
     */
    public static ActorInfo getCurrentActor() {
        return currentActor.get();
    }

    /**
     * Récupère le sub (subject/ID) de l'utilisateur courant
     * @return Le sub de l'utilisateur, ou null si aucun utilisateur n'est authentifié
     */
    public static String getCurrentUserSub() {
        ActorInfo actor = currentActor.get();
        return actor != null ? actor.getSub() : null;
    }

    /**
     * Nettoie le contexte (appelé automatiquement par l'interceptor)
     */
    public static void clear() {
        currentActor.remove();
    }

    /**
     * Informations sur l'utilisateur courant extraites du JWT
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActorInfo {
        /**
         * Subject - Identifiant unique de l'utilisateur (généralement depuis Keycloak)
         */
        private String sub;

        /**
         * Email de l'utilisateur
         */
        private String email;

        /**
         * Nom d'utilisateur (username/login)
         */
        private String username;

        /**
         * Prénom de l'utilisateur
         */
        private String firstName;

        /**
         * Nom de famille de l'utilisateur
         */
        private String lastName;

        /**
         * Rôle principal de l'utilisateur (ADMIN, USER, etc.)
         */
        private String role;
    }
}
