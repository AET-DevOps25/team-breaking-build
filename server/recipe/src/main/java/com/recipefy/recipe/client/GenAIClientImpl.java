package com.recipefy.recipe.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            String url = genaiServiceUrl + "/genai/vector/index";
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> recipe = new HashMap<>();
            
            // Create filtered metadata with only fields needed for vector search
            Map<String, Object> filteredMetadata = new HashMap<>();
            filteredMetadata.put("id", metadata.getId());
            filteredMetadata.put("title", metadata.getTitle());
            filteredMetadata.put("description", metadata.getDescription());
            filteredMetadata.put("servingSize", metadata.getServingSize());
            filteredMetadata.put("tags", metadata.getTags());
            
            recipe.put("metadata", filteredMetadata);
            
            if (details != null) {
                // Create filtered details with only fields needed for vector search
                Map<String, Object> filteredDetails = new HashMap<>();
                filteredDetails.put("servingSize", details.getServingSize());
                filteredDetails.put("recipeIngredients", details.getRecipeIngredients());
                
                // Filter recipe steps to remove image data
                List<Map<String, Object>> filteredSteps = new ArrayList<>();
                if (details.getRecipeSteps() != null) {
                    for (var step : details.getRecipeSteps()) {
                        Map<String, Object> filteredStep = new HashMap<>();
                        filteredStep.put("order", step.getOrder());
                        filteredStep.put("details", step.getDetails());
                        filteredSteps.add(filteredStep);
                    }
                }
                filteredDetails.put("recipeSteps", filteredSteps);
                recipe.put("details", filteredDetails);
            } else {
                // For copied recipes without details, create a minimal details object
                Map<String, Object> minimalDetails = new HashMap<>();
                minimalDetails.put("servingSize", metadata.getServingSize() != null ? metadata.getServingSize() : 1);
                minimalDetails.put("recipeIngredients", new ArrayList<>());
                minimalDetails.put("recipeSteps", new ArrayList<>());
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
            String url = genaiServiceUrl + "/genai/vector/" + recipeId;
            restTemplate.delete(url);
            log.info("Successfully deleted recipe {} from GenAI service", recipeId);
        } catch (Exception e) {
            log.error("Failed to delete recipe {} from GenAI service: {}", recipeId, e.getMessage());
            // Don't throw exception to avoid breaking the main flow
        }
    }
} 
