package com.recipefy.recipe.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.recipefy.recipe.exception.UnauthorizedException;
import com.recipefy.recipe.exception.ValidationException;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.CreateRecipeRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import com.recipefy.recipe.service.RecipeService;
import com.recipefy.recipe.util.HeaderUtil;

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
        testRecipeDTO.setCreatedAt(now);
        testRecipeDTO.setUpdatedAt(now);
        testRecipeDTO.setTitle("Test Recipe");
        testRecipeDTO.setDescription("Test Description");
        testRecipeDTO.setThumbnail("test.jpg".getBytes());
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

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            ResponseEntity<RecipeMetadataDTO> response = recipeController.getRecipe(1L);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(testRecipeDTO.getId(), response.getBody().getId());
            verify(recipeService).getRecipe(1L);
        }
    }

    @Test
    void getRecipe_WhenRecipeDoesNotExist_ShouldReturnNotFound() {
        when(recipeService.getRecipe(1L)).thenThrow(new EntityNotFoundException("Recipe not found"));

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            ResponseEntity<RecipeMetadataDTO> response = recipeController.getRecipe(1L);

            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(response.getBody());
            verify(recipeService).getRecipe(1L);
        }
    }

    @Test
    void createRecipe_ShouldCreateAndReturnRecipe() {
        CreateRecipeRequest request = new CreateRecipeRequest(testRecipeDTO, new InitRecipeRequest(null));
        when(recipeService.createRecipe(any(CreateRecipeRequest.class), any(UUID.class)))
            .thenReturn(testRecipeDTO);

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            ResponseEntity<RecipeMetadataDTO> response = recipeController.createRecipe(request);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(testRecipeDTO.getId(), response.getBody().getId());
            verify(recipeService).createRecipe(any(CreateRecipeRequest.class), any(UUID.class));
        }
    }

    @Test
    void updateRecipe_WhenRecipeExists_ShouldUpdateAndReturnRecipe() {
        // Arrange
        when(recipeService.updateRecipe(anyLong(), any(RecipeMetadataDTO.class), any(UUID.class))).thenReturn(testRecipeDTO);

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            // Act
            ResponseEntity<RecipeMetadataDTO> response = recipeController.updateRecipe(1L, testRecipeDTO);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(testRecipeDTO.getId(), response.getBody().getId());
            verify(recipeService).updateRecipe(1L, testRecipeDTO, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        }
    }

    @Test
    void updateRecipe_WhenRecipeDoesNotExist_ShouldReturnNotFound() {
        when(recipeService.updateRecipe(anyLong(), any(RecipeMetadataDTO.class), any(UUID.class)))
            .thenThrow(new EntityNotFoundException("Recipe not found"));

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            ResponseEntity<RecipeMetadataDTO> response = recipeController.updateRecipe(1L, testRecipeDTO);

            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(response.getBody());
            verify(recipeService).updateRecipe(1L, testRecipeDTO, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        }
    }

    @Test
    void copyRecipe_ShouldCreateCopyAndReturnRecipe() {
        when(recipeService.copyRecipe(anyLong(), any(UUID.class), anyLong())).thenReturn(testRecipeDTO);

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

            ResponseEntity<RecipeMetadataDTO> response = recipeController.copyRecipe(1L, 3L);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(testRecipeDTO.getId(), response.getBody().getId());
            verify(recipeService).copyRecipe(1L, UUID.fromString("550e8400-e29b-41d4-a716-446655440001"), 3L);
        }
    }

    @Test
    void deleteRecipe_ShouldDeleteRecipe() {
        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            ResponseEntity<Void> response = recipeController.deleteRecipe(1L);

            assertNotNull(response);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Test
    void updateRecipe_WhenUserNotOwner_ShouldReturnForbidden() {
        when(recipeService.updateRecipe(anyLong(), any(RecipeMetadataDTO.class), any(UUID.class)))
            .thenThrow(new UnauthorizedException("You don't have permission to access this recipe"));

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

            ResponseEntity<RecipeMetadataDTO> response = recipeController.updateRecipe(1L, testRecipeDTO);

            assertNotNull(response);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNull(response.getBody());
            verify(recipeService).updateRecipe(1L, testRecipeDTO, UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        }
    }

    @Test
    void deleteRecipe_WhenUserNotOwner_ShouldReturnForbidden() {
        doThrow(new UnauthorizedException("You don't have permission to access this recipe"))
            .when(recipeService).deleteRecipe(anyLong(), any(UUID.class));

        try (var headerUtilMock = mockStatic(HeaderUtil.class)) {
            headerUtilMock.when(HeaderUtil::extractRequiredUserIdFromHeader).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

            ResponseEntity<Void> response = recipeController.deleteRecipe(1L);

            assertNotNull(response);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNull(response.getBody());
            verify(recipeService).deleteRecipe(1L, UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        }
    }
} 