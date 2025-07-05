package com.recipefy.recipe.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.recipefy.recipe.client.GenAIClient;
import com.recipefy.recipe.client.VersionClient;
import com.recipefy.recipe.mapper.dto.RecipeMetadataDTOMapper;
import com.recipefy.recipe.model.dto.BranchDTO;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;
import com.recipefy.recipe.model.entity.RecipeTag;
import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.CreateRecipeRequest;
import com.recipefy.recipe.repository.RecipeRepository;
import com.recipefy.recipe.repository.TagRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeServiceImpl implements RecipeService {
    private final RecipeRepository recipeRepository;
    private final TagRepository recipeTagRepository;
    private final VersionClient versionClient;
    private final GenAIClient genAIClient;

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
    public RecipeMetadataDTO createRecipe(CreateRecipeRequest request) {
        RecipeMetadata recipe = RecipeMetadataDTOMapper.toEntity(request.getMetadata());
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        // Ensure tags exist or are created
        Set<RecipeTag> tags = request.getMetadata().getTags().stream()
                .map(dto -> recipeTagRepository.findByName(dto.getName())
                        .orElseGet(() -> recipeTagRepository.save(new RecipeTag(dto.getName(), new HashSet<>())))
                ).collect(Collectors.toSet());
        recipe.setTags(tags);

        // Save recipe first to get the ID
        RecipeMetadata saved = recipeRepository.save(recipe);
        
        // Initialize version control and get branch info
        BranchDTO branch = versionClient.initRecipe(saved.getId(), request.getInitRequest());
        log.info("Initialized recipe {} with branch ID: {}", saved.getId(), branch.getId());
        
        // Trigger GenAI indexing
        try {
            genAIClient.indexRecipe(RecipeMetadataDTOMapper.toDTO(saved), request.getInitRequest().getRecipeDetails());
            log.info("Successfully triggered GenAI indexing for recipe {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to trigger GenAI indexing for recipe {}: {}", saved.getId(), e.getMessage());
        }
        
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

        // Save copy first to get the ID
        RecipeMetadata saved = recipeRepository.save(copy);
        
        // Copy version control and get branch info
        BranchDTO branch = versionClient.copyRecipe(branchId, new CopyBranchRequest(saved.getId()));
        log.info("Copied recipe {} with new branch ID: {}", saved.getId(), branch.getId());
        
        // Trigger GenAI indexing for the copied recipe
        try {
            // Note: For copied recipes, we need to get the recipe details from the version service
            // For now, we'll just index the metadata. In a real implementation, you might want to
            // fetch the recipe details from the version service
            genAIClient.indexRecipe(RecipeMetadataDTOMapper.toDTO(saved), null);
            log.info("Successfully triggered GenAI indexing for copied recipe {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to trigger GenAI indexing for copied recipe {}: {}", saved.getId(), e.getMessage());
        }
        
        return RecipeMetadataDTOMapper.toDTO(saved);
    }

    @Override
    public void deleteRecipe(Long recipeId) {
        // Trigger GenAI deletion before deleting from database
        try {
            genAIClient.deleteRecipe(recipeId.toString());
            log.info("Successfully triggered GenAI deletion for recipe {}", recipeId);
        } catch (Exception e) {
            log.error("Failed to trigger GenAI deletion for recipe {}: {}", recipeId, e.getMessage());
        }
        
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