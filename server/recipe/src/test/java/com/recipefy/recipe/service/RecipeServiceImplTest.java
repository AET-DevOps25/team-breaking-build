package com.recipefy.recipe.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.recipefy.recipe.client.GenAIClient;
import com.recipefy.recipe.client.VersionClient;
import com.recipefy.recipe.mapper.dto.RecipeMetadataDTOMapper;
import com.recipefy.recipe.model.dto.BranchDTO;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;
import com.recipefy.recipe.model.entity.RecipeTag;
import com.recipefy.recipe.model.request.CreateRecipeRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import com.recipefy.recipe.repository.RecipeRepository;
import com.recipefy.recipe.repository.TagRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private VersionClient versionClient;

    @Mock
    private GenAIClient genAIClient;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    private RecipeMetadata testRecipe;
    private RecipeMetadataDTO testRecipeDTO;
    private RecipeTag testTag;
    private RecipeTagDTO testTagDTO;
    private BranchDTO testBranchDTO;

    @BeforeEach
    void setUp() {
        // Setup test recipe
        testRecipe = new RecipeMetadata();
        testRecipe.setId(1L);
        testRecipe.setUserId(1L);
        testRecipe.setTitle("Test Recipe");
        testRecipe.setDescription("Test Description");
        testRecipe.setThumbnail("test.jpg");
        testRecipe.setServingSize(4);
        testRecipe.setCreatedAt(LocalDateTime.now());
        testRecipe.setUpdatedAt(LocalDateTime.now());

        // Setup test tag
        testTag = new RecipeTag();
        testTag.setId(1L);
        testTag.setName("Test Tag");
        testTag.setRecipes(new HashSet<>());

        testRecipe.setTags(new HashSet<>(Collections.singletonList(testTag)));

        // Setup DTOs
        testRecipeDTO = RecipeMetadataDTOMapper.toDTO(testRecipe);
        testTagDTO = new RecipeTagDTO(1L, "Test Tag");
        
        // Setup test branch
        testBranchDTO = new BranchDTO(1L, "main", 1L, 1L, LocalDateTime.now());
    }

    @Test
    void getAllRecipes_ShouldReturnPageOfRecipes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecipeMetadata> recipePage = new PageImpl<>(Collections.singletonList(testRecipe));
        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);

        // Act
        Page<RecipeMetadataDTO> result = recipeService.getAllRecipes(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testRecipeDTO.getId(), result.getContent().get(0).getId());
        verify(recipeRepository).findAll(pageable);
    }

    @Test
    void getRecipe_WhenRecipeExists_ShouldReturnRecipe() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        // Act
        RecipeMetadataDTO result = recipeService.getRecipe(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testRecipeDTO.getId(), result.getId());
        assertEquals(testRecipeDTO.getTitle(), result.getTitle());
        verify(recipeRepository).findById(1L);
    }

    @Test
    void getRecipe_WhenRecipeDoesNotExist_ShouldThrowException() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> recipeService.getRecipe(1L));
        verify(recipeRepository).findById(1L);
    }

    @Test
    void createRecipe_ShouldCreateAndReturnRecipe() {
        // Arrange
        CreateRecipeRequest request = new CreateRecipeRequest(testRecipeDTO, new InitRecipeRequest(1L, null));
        when(recipeRepository.save(any(RecipeMetadata.class))).thenReturn(testRecipe);
        when(tagRepository.findByName(anyString())).thenReturn(Optional.of(testTag));
        when(versionClient.initRecipe(anyLong(), any(InitRecipeRequest.class))).thenReturn(testBranchDTO);
        doNothing().when(genAIClient).indexRecipe(any(RecipeMetadataDTO.class), any());

        // Act
        RecipeMetadataDTO result = recipeService.createRecipe(request);

        // Assert
        assertNotNull(result);
        assertEquals(testRecipeDTO.getId(), result.getId());
        assertEquals(testRecipeDTO.getTitle(), result.getTitle());
        verify(recipeRepository).save(any(RecipeMetadata.class));
        verify(versionClient).initRecipe(anyLong(), any(InitRecipeRequest.class));
        verify(genAIClient).indexRecipe(any(RecipeMetadataDTO.class), any());
    }

    @Test
    void updateRecipe_WhenRecipeExists_ShouldUpdateAndReturnRecipe() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(recipeRepository.save(any(RecipeMetadata.class))).thenReturn(testRecipe);

        // Act
        RecipeMetadataDTO result = recipeService.updateRecipe(1L, testRecipeDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testRecipeDTO.getId(), result.getId());
        assertEquals(testRecipeDTO.getTitle(), result.getTitle());
        verify(recipeRepository).findById(1L);
        verify(recipeRepository).save(any(RecipeMetadata.class));
    }

    @Test
    void updateRecipe_WhenRecipeDoesNotExist_ShouldThrowException() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> recipeService.updateRecipe(1L, testRecipeDTO));
        verify(recipeRepository).findById(1L);
        verify(recipeRepository, never()).save(any(RecipeMetadata.class));
    }

    @Test
    void copyRecipe_ShouldCreateCopyAndReturnRecipe() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(recipeRepository.save(any(RecipeMetadata.class))).thenReturn(testRecipe);
        when(versionClient.copyRecipe(anyLong(), any())).thenReturn(testBranchDTO);
        doNothing().when(genAIClient).indexRecipe(any(RecipeMetadataDTO.class), any());

        // Act
        RecipeMetadataDTO result = recipeService.copyRecipe(1L, 2L, 3L);

        // Assert
        assertNotNull(result);
        assertEquals(testRecipeDTO.getId(), result.getId());
        assertEquals(testRecipeDTO.getTitle(), result.getTitle());
        verify(recipeRepository).findById(1L);
        verify(recipeRepository).save(any(RecipeMetadata.class));
        verify(versionClient).copyRecipe(anyLong(), any());
        verify(genAIClient).indexRecipe(any(RecipeMetadataDTO.class), any());
    }

    @Test
    void deleteRecipe_ShouldDeleteRecipe() {
        // Arrange
        doNothing().when(recipeRepository).deleteById(1L);
        doNothing().when(genAIClient).deleteRecipe(anyString());

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> recipeService.deleteRecipe(1L));
        verify(recipeRepository).deleteById(1L);
        verify(genAIClient).deleteRecipe(anyString());
    }

    @Test
    void updateTags_WhenRecipeExists_ShouldUpdateTagsAndReturnRecipe() {
        // Arrange
        List<RecipeTagDTO> newTags = Collections.singletonList(testTagDTO);
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(tagRepository.findByName(anyString())).thenReturn(Optional.of(testTag));
        when(recipeRepository.save(any(RecipeMetadata.class))).thenReturn(testRecipe);

        // Act
        RecipeMetadataDTO result = recipeService.updateTags(1L, newTags);

        // Assert
        assertNotNull(result);
        assertEquals(testRecipeDTO.getId(), result.getId());
        assertEquals(1, result.getTags().size());
        verify(recipeRepository).findById(1L);
        verify(tagRepository).findByName(anyString());
        verify(recipeRepository).save(any(RecipeMetadata.class));
    }

    @Test
    void updateTags_WhenRecipeDoesNotExist_ShouldThrowException() {
        // Arrange
        List<RecipeTagDTO> newTags = Collections.singletonList(testTagDTO);
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> recipeService.updateTags(1L, newTags));
        verify(recipeRepository).findById(1L);
        verify(recipeRepository, never()).save(any(RecipeMetadata.class));
    }
} 