package com.recipefy.recipe.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import com.recipefy.recipe.service.RecipeService;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private RecipeController recipeController;

    private RecipeMetadataDTO testRecipeDTO;
    private List<RecipeTagDTO> testTags;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        testRecipeDTO = new RecipeMetadataDTO();
        testRecipeDTO.setId(1L);
        testRecipeDTO.setUserId(1L);
        testRecipeDTO.setCreatedAt(now);
        testRecipeDTO.setUpdatedAt(now);
        testRecipeDTO.setTitle("Test Recipe");
        testRecipeDTO.setDescription("Test Description");
        testRecipeDTO.setServingSize(4);
        testRecipeDTO.setTags(Collections.emptyList());
        
        testTags = Collections.singletonList(new RecipeTagDTO(1L, "Test Tag"));
    }

    @Test
    void getAllRecipes_ShouldReturnPageOfRecipes() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecipeMetadataDTO> recipePage = new PageImpl<>(Collections.singletonList(testRecipeDTO));
        when(recipeService.getAllRecipes(pageable)).thenReturn(recipePage);

        ResponseEntity<Page<RecipeMetadataDTO>> response = recipeController.getAllRecipes(pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(recipeService).getAllRecipes(pageable);
    }

    @Test
    void getRecipe_WhenRecipeExists_ShouldReturnRecipe() {
        when(recipeService.getRecipe(1L)).thenReturn(testRecipeDTO);

        ResponseEntity<RecipeMetadataDTO> response = recipeController.getRecipe(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testRecipeDTO.getId(), response.getBody().getId());
        verify(recipeService).getRecipe(1L);
    }

    @Test
    void getRecipe_WhenRecipeDoesNotExist_ShouldReturnNotFound() {
        when(recipeService.getRecipe(1L)).thenThrow(new EntityNotFoundException("Recipe not found"));

        ResponseEntity<RecipeMetadataDTO> response = recipeController.getRecipe(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(recipeService).getRecipe(1L);
    }

    @Test
    void createRecipe_ShouldCreateAndReturnRecipe() {
        when(recipeService.createRecipe(any(RecipeMetadataDTO.class), any(InitRecipeRequest.class)))
            .thenReturn(testRecipeDTO);

        ResponseEntity<RecipeMetadataDTO> response = recipeController.createRecipe(testRecipeDTO, new InitRecipeRequest(1L, null));

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testRecipeDTO.getId(), response.getBody().getId());
        verify(recipeService).createRecipe(any(RecipeMetadataDTO.class), any(InitRecipeRequest.class));
    }

    @Test
    void updateRecipe_WhenRecipeExists_ShouldUpdateAndReturnRecipe() {
        // Arrange
        when(recipeService.updateRecipe(anyLong(), any(RecipeMetadataDTO.class))).thenReturn(testRecipeDTO);

        // Act
        ResponseEntity<RecipeMetadataDTO> response = recipeController.updateRecipe(1L, testRecipeDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testRecipeDTO.getId(), response.getBody().getId());
        verify(recipeService).updateRecipe(1L, testRecipeDTO);
    }

    @Test
    void updateRecipe_WhenRecipeDoesNotExist_ShouldReturnNotFound() {
        when(recipeService.updateRecipe(anyLong(), any(RecipeMetadataDTO.class)))
            .thenThrow(new EntityNotFoundException("Recipe not found"));

        ResponseEntity<RecipeMetadataDTO> response = recipeController.updateRecipe(1L, testRecipeDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(recipeService).updateRecipe(1L, testRecipeDTO);
    }

    @Test
    void copyRecipe_ShouldCreateCopyAndReturnRecipe() {
        when(recipeService.copyRecipe(anyLong(), anyLong(), anyLong())).thenReturn(testRecipeDTO);

        ResponseEntity<RecipeMetadataDTO> response = recipeController.copyRecipe(1L, 2L, 3L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testRecipeDTO.getId(), response.getBody().getId());
        verify(recipeService).copyRecipe(1L, 2L, 3L);
    }

    /** TODO: OPEN WHEN VCS IMPLEMENTED DELETE
    @Test
    void deleteRecipe_ShouldReturnNoContent() {
        doThrow(new UnsupportedOperationException("Delete not supported")).when(recipeService).deleteRecipe(1L);

        ResponseEntity<Void> response = recipeController.deleteRecipe(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(recipeService).deleteRecipe(1L);
    }
    */

    @Test
    void updateTags_WhenRecipeExists_ShouldUpdateTagsAndReturnRecipe() {
        when(recipeService.updateTags(anyLong(), anyList())).thenReturn(testRecipeDTO);

        ResponseEntity<RecipeMetadataDTO> response = recipeController.updateTags(1L, testTags);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testRecipeDTO.getId(), response.getBody().getId());
        verify(recipeService).updateTags(1L, testTags);
    }

    @Test
    void updateTags_WhenRecipeDoesNotExist_ShouldReturnNotFound() {
        when(recipeService.updateTags(anyLong(), anyList()))
            .thenThrow(new EntityNotFoundException("Recipe not found"));

        ResponseEntity<RecipeMetadataDTO> response = recipeController.updateTags(1L, testTags);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(recipeService).updateTags(1L, testTags);
    }
} 