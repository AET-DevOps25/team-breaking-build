package com.recipefy.recipe.client;

import org.springframework.beans.factory.annotation.Value;
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
    public BranchDTO initRecipe(Long recipeId, InitRecipeRequest request) {
        String url = vcsServiceUrl + "/vcs/recipes/" + recipeId + "/init";
        return restTemplate.postForEntity(url, request, BranchDTO.class).getBody();
    }

    @Override
    public BranchDTO copyRecipe(Long branchId, CopyBranchRequest request) {
        String url = vcsServiceUrl + "/vcs/branches/" + branchId + "/copy";
        return restTemplate.postForEntity(url, request, BranchDTO.class).getBody();
    }
}
