package com.recipefy.recipe.client;

import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class VersionClientImpl implements VersionClient {
    private final RestTemplate restTemplate;

    @Value("${vcs.service.url}")
    private String vcsServiceUrl;

    @Override
    public void initRecipe(Long recipeId, InitRecipeRequest request) {
        String url = vcsServiceUrl + "/recipes/" + recipeId;
        restTemplate.postForEntity(url, request, Void.class);
    }

    @Override
    public void copyRecipe(Long branchId, CopyBranchRequest request) {
        String url = vcsServiceUrl + "/branches/" + branchId + "/copy";
        restTemplate.postForEntity(url, request, Void.class);
    }
}
