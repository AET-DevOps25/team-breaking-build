package com.recipefy.recipe.client;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.recipefy.recipe.model.dto.BranchDTO;
import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VersionClientImpl implements VersionClient {
    private final RestTemplate restTemplate;

    @Value("${vcs.service.url}")
    private String vcsServiceUrl;

    @Override
    public BranchDTO initRecipe(Long recipeId, InitRecipeRequest request, UUID userId) {
        String url = vcsServiceUrl + "/vcs/recipes/" + recipeId + "/init";
        
        // Create headers with userId and request ID
        HttpHeaders headers = createHeaders(userId);
        
        // Create HTTP entity with request body and headers
        HttpEntity<InitRecipeRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<BranchDTO> response = restTemplate.exchange(url, HttpMethod.POST, entity, BranchDTO.class);
        return response.getBody();
    }

    @Override
    public BranchDTO copyRecipe(Long branchId, CopyBranchRequest request, UUID userId) {
        String url = vcsServiceUrl + "/vcs/branches/" + branchId + "/copy";
        
        // Create headers with userId and request ID
        HttpHeaders headers = createHeaders(userId);
        
        // Create HTTP entity with request body and headers
        HttpEntity<CopyBranchRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<BranchDTO> response = restTemplate.exchange(url, HttpMethod.POST, entity, BranchDTO.class);
        return response.getBody();
    }

    /**
     * Create HTTP headers with user ID and request ID for version service requests
     */
    private HttpHeaders createHeaders(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        
        // Add user ID header
        headers.set("X-User-Id", userId.toString());
        
        // Add request ID header if available from MDC
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.trim().isEmpty()) {
            headers.set("X-Request-ID", requestId);
        }
        
        return headers;
    }
}
