package com.recipefy.recipe.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.recipefy.recipe.model.dto.RecipeDetailsDTO;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenAIClientImpl implements GenAIClient {
    private final RestTemplate restTemplate;

    @Value("${genai.service.url}")
    private String genaiServiceUrl;

    @Override
    public void indexRecipe(RecipeMetadataDTO metadata, RecipeDetailsDTO details) {
        try {
            String url = genaiServiceUrl + "/api/v1/recipes/index";
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> recipe = new HashMap<>();
            recipe.put("metadata", metadata);
            if (details != null) {
                recipe.put("details", details);
            } else {
                // For copied recipes without details, create a minimal details object
                Map<String, Object> minimalDetails = new HashMap<>();
                minimalDetails.put("servingSize", metadata.getServingSize() != null ? metadata.getServingSize() : 1);
                minimalDetails.put("recipeIngredients", new ArrayList<>());
                minimalDetails.put("recipeSteps", new ArrayList<>());
                minimalDetails.put("images", new ArrayList<>());
                recipe.put("details", minimalDetails);
            }
            requestBody.put("recipe", recipe);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity(url, request, Object.class);
            log.info("Successfully indexed recipe {} in GenAI service", metadata.getId());
        } catch (Exception e) {
            log.error("Failed to index recipe {} in GenAI service: {}", metadata.getId(), e.getMessage());
            // Don't throw exception to avoid breaking the main flow
        }
    }

    @Override
    public void deleteRecipe(String recipeId) {
        try {
            String url = genaiServiceUrl + "/api/v1/recipes/" + recipeId;
            restTemplate.delete(url);
            log.info("Successfully deleted recipe {} from GenAI service", recipeId);
        } catch (Exception e) {
            log.error("Failed to delete recipe {} from GenAI service: {}", recipeId, e.getMessage());
            // Don't throw exception to avoid breaking the main flow
        }
    }
} 
