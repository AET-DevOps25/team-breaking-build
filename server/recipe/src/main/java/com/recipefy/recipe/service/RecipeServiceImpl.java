package com.recipefy.recipe.service;

import com.recipefy.recipe.client.GenAIClient;
import com.recipefy.recipe.client.VersionClient;
import com.recipefy.recipe.exception.UnauthorizedException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeServiceImpl implements RecipeService {
    private final RecipeRepository recipeRepository;
    private final TagRepository recipeTagRepository;
    private final VersionClient versionClient;
    private final GenAIClient genAIClient;

    @Override
    public Page<RecipeMetadataDTO> getAllRecipes(UUID userId, Pageable pageable) {
        return recipeRepository.findAllByUserId(userId, pageable)
                .map(RecipeMetadataDTOMapper::toDTO);
    }

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
    @Transactional
    public RecipeMetadataDTO createRecipe(CreateRecipeRequest request, UUID userId) {
        RecipeMetadata recipe = RecipeMetadataDTOMapper.toEntity(request.getMetadata(), userId);

        // Only use tag IDs from the client
        Set<RecipeTag> tags = request.getMetadata().getTags().stream()
            .map(dto -> recipeTagRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Tag not found with ID: " + dto.getId())))
            .collect(Collectors.toSet());
        recipe.setTags(tags);

        // Save recipe first to get the ID
        RecipeMetadata saved = recipeRepository.save(recipe);
        
        // Initialize version control and get branch info
        BranchDTO branch = versionClient.initRecipe(saved.getId(), request.getInitRequest(), userId);
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

    /**
     * Validates that the user owns the recipe
     */
    private RecipeMetadata validateRecipeOwnership(Long recipeId, UUID userId) {
        RecipeMetadata recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found with ID: " + recipeId));
        
        if (!recipe.getUserId().equals(userId)) {
            log.warn("User {} attempted to access recipe {} which they don't own", userId, recipeId);
            throw new UnauthorizedException("You don't have permission to access this recipe");
        }
        
        return recipe;
    }

    @Override
    @Transactional
    public RecipeMetadataDTO updateRecipe(Long recipeId, RecipeMetadataDTO metadataDTO, UUID userId) {
        RecipeMetadata existing = validateRecipeOwnership(recipeId, userId);

        existing.setTitle(metadataDTO.getTitle());
        existing.setDescription(metadataDTO.getDescription());
        
        // Convert RecipeImageDTO to byte[] for thumbnail
        if (metadataDTO.getThumbnail() != null) {
            existing.setThumbnail(metadataDTO.getThumbnail().getBase64String());
        } else {
            existing.setThumbnail(null);
        }
        
        existing.setServingSize(metadataDTO.getServingSize());

        // Update tags here
        if (metadataDTO.getTags() != null && !metadataDTO.getTags().isEmpty()) {
            Set<RecipeTag> tags = metadataDTO.getTags().stream()
                .map(dto -> recipeTagRepository.findById(dto.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Tag not found with ID: " + dto.getId())))
                .collect(Collectors.toSet());
            existing.setTags(tags);
        }

        RecipeMetadata updated = recipeRepository.save(existing);
        return RecipeMetadataDTOMapper.toDTO(updated);
    }

    @Override
    @Transactional
    public RecipeMetadataDTO copyRecipe(Long recipeId, UUID userId, Long branchId) {
        RecipeMetadata original = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Original recipe not found"));

        RecipeMetadata copy = new RecipeMetadata();
        copy.setUserId(userId);
        copy.setForkedFrom(original.getId());
        copy.setTitle(original.getTitle());
        copy.setDescription(original.getDescription());
        copy.setThumbnail(original.getThumbnail()); // byte[] to byte[] - no conversion needed
        copy.setServingSize(original.getServingSize());
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
    public void deleteRecipe(Long recipeId, UUID userId) {
        // Validate ownership before deletion
        validateRecipeOwnership(recipeId, userId);
        
        // Trigger GenAI deletion before deleting from database
        try {
            genAIClient.deleteRecipe(recipeId.toString());
            log.info("Successfully triggered GenAI deletion for recipe {}", recipeId);
        } catch (Exception e) {
            log.error("Failed to trigger GenAI deletion for recipe {}: {}", recipeId, e.getMessage());
        }
        
        recipeRepository.deleteById(recipeId);
        log.info("Successfully deleted recipe {} by user {}", recipeId, userId);
        throw new UnsupportedOperationException("VCS delete functionality is not implemented yet.");
    }

    @Override
    public List<RecipeTagDTO> getAllTags() {
        return recipeTagRepository.findAll().stream()
            .map(tag -> new RecipeTagDTO(tag.getId(), tag.getName()))
            .collect(Collectors.toList());
    }
}