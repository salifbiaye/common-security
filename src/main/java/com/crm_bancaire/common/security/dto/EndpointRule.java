package com.crm_bancaire.common.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO représentant une règle de sécurité pour un endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointRule {
    /**
     * Chemin de base du service (ex: /api/users)
     */
    private String basePath;

    /**
     * Chemin relatif de l'endpoint (ex: /{id})
     */
    private String path;

    /**
     * Méthodes HTTP autorisées (GET, POST, PUT, DELETE, PATCH)
     */
    private List<String> methods;

    /**
     * Rôles autorisés à accéder à cet endpoint
     */
    private List<String> roles;

    /**
     * Indique si l'endpoint est public (pas d'authentification requise)
     */
    private boolean isPublic;

    /**
     * Retourne le chemin complet de l'endpoint
     */
    public String getFullPath() {
        // Si path est vide, retourner basePath sans slash final
        if (path == null || path.isEmpty()) {
            return basePath;
        }

        // Si basePath se termine par / et path commence par /, enlever un /
        if (basePath.endsWith("/") && path.startsWith("/")) {
            return basePath + path.substring(1);
        }
        // Si aucun des deux n'a de /, ajouter un /
        else if (!basePath.endsWith("/") && !path.startsWith("/")) {
            return basePath + "/" + path;
        }
        // Sinon, juste concaténer
        return basePath + path;
    }
}
