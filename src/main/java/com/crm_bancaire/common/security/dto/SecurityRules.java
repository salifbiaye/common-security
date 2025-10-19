package com.crm_bancaire.common.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO contenant toutes les règles de sécurité d'un microservice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityRules {
    /**
     * Nom du service (ex: user-service)
     */
    private String serviceName;

    /**
     * Chemin de base du service (ex: /api/users)
     */
    private String basePath;

    /**
     * Liste des règles de sécurité pour tous les endpoints du service
     */
    private List<EndpointRule> endpoints;
}
