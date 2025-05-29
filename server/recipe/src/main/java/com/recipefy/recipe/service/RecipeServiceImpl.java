package com.recipefy.recipe.service;

import com.recipefy.recipe.client.VCSClient;
import com.recipefy.recipe.mapper.dto.RecipeMetadataDTOMapper;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;
import com.recipefy.recipe.model.entity.RecipeTag;
import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import com.recipefy.recipe.repository.RecipeRepository;
import com.recipefy.recipe.repository.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {
    private final RecipeRepository recipeRepository;
    private final TagRepository recipeTagRepository;
    private final VCSClient vcsClient;

    @Override
    public Page<RecipeMetadataDTO> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable)
                .map(RecipeMetadataDTOMapper::toDTO);
    }

    @Override
    public RecipeMetadataDTO getRecipe(Long recipeId) {
        RecipeMetadata recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found with ID: " + recipeId));
        return RecipeMetadataDTOMapper.toDTO(recipe);
    }

    @Override
    public RecipeMetadataDTO createRecipe(RecipeMetadataDTO metadataDTO, InitRecipeRequest request) {
        RecipeMetadata recipe = RecipeMetadataDTOMapper.toEntity(metadataDTO);
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        // Ensure tags exist or are created
        Set<RecipeTag> tags = metadataDTO.getTags().stream()
                .map(dto -> recipeTagRepository.findByName(dto.getName())
                        .orElseGet(() -> recipeTagRepository.save(new RecipeTag(dto.getName(), new HashSet<>())))
                ).collect(Collectors.toSet());
        recipe.setTags(tags);

        vcsClient.initRecipe(recipe.getId(), request);

        RecipeMetadata saved = recipeRepository.save(recipe);
        return RecipeMetadataDTOMapper.toDTO(saved);
    }

    @Override
    public RecipeMetadataDTO updateRecipe(Long recipeId, RecipeMetadataDTO metadataDTO) {
        RecipeMetadata existing = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found with ID: " + recipeId));

        existing.setTitle(metadataDTO.getTitle());
        existing.setDescription(metadataDTO.getDescription());
        existing.setThumbnail(metadataDTO.getThumbnail().getUrl());
        existing.setServingSize(metadataDTO.getServingSize());
        existing.setUpdatedAt(LocalDateTime.now());

        // Tags are not updated here, use updateTags method instead
        RecipeMetadata updated = recipeRepository.save(existing);
        return RecipeMetadataDTOMapper.toDTO(updated);
    }

    @Override
    public RecipeMetadataDTO copyRecipe(Long recipeId, Long userId, Long branchId) {
        RecipeMetadata original = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Original recipe not found"));

        RecipeMetadata copy = new RecipeMetadata();
        copy.setUserId(userId);
        copy.setForkedFrom(original.getId());
        copy.setTitle(original.getTitle());
        copy.setDescription(original.getDescription());
        copy.setThumbnail(original.getThumbnail());
        copy.setServingSize(original.getServingSize());
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        copy.setTags(original.getTags()); // safe since Set<RecipeTag> is reused

        vcsClient.copyRecipe(branchId, new CopyBranchRequest(recipeId));

        RecipeMetadata saved = recipeRepository.save(copy);
        return RecipeMetadataDTOMapper.toDTO(saved);
    }

    @Override
    public void deleteRecipe(Long recipeId) {
        recipeRepository.deleteById(recipeId);
        throw new UnsupportedOperationException("VCS delete functionality is not implemented yet.");
    }

    @Override
    public RecipeMetadataDTO updateTags(Long recipeId, List<RecipeTagDTO> tagDTOs) {
        RecipeMetadata recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

        Set<RecipeTag> tags = tagDTOs.stream()
                .map(dto -> recipeTagRepository.findByName(dto.getName())
                        .orElseGet(() -> recipeTagRepository.save(new RecipeTag(dto.getName(), new HashSet<>())))
                ).collect(Collectors.toSet());

        recipe.setTags(tags);
        return RecipeMetadataDTOMapper.toDTO(recipeRepository.save(recipe));
    }
}