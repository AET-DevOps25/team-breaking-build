package com.recipefy.recipe.client;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.recipefy.recipe.model.dto.BranchDTO;
import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;

@ExtendWith(MockitoExtension.class)
class VersionClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private VersionClientImpl versionClient;

    private final String vcsServiceUrl = "http://localhost:8082";
    private final UUID testUserId = UUID.randomUUID();
    private final String testRequestId = "test-request-123";

    @BeforeEach
    void setUp() {
        versionClient = new VersionClientImpl(restTemplate);
        ReflectionTestUtils.setField(versionClient, "vcsServiceUrl", vcsServiceUrl);
        
        // Clear MDC before each test
        MDC.clear();
    }

    @Test
    void initRecipe_ShouldIncludeRequestIdHeader_WhenRequestIdIsInMDC() {
        // Given
        Long recipeId = 1L;
        InitRecipeRequest request = new InitRecipeRequest(null);
        BranchDTO mockResponse = new BranchDTO();
        mockResponse.setId(1L);
        
        // Set request ID in MDC
        MDC.put("requestId", testRequestId);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(BranchDTO.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        BranchDTO result = versionClient.initRecipe(recipeId, request, testUserId);

        // Then
        assertNotNull(result);
        
        // Verify the request was made with correct headers
        ArgumentCaptor<HttpEntity<InitRecipeRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(vcsServiceUrl + "/vcs/recipes/" + recipeId + "/init"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(BranchDTO.class)
        );
        
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertEquals(testUserId.toString(), headers.getFirst("X-User-Id"));
        assertEquals(testRequestId, headers.getFirst("X-Request-ID"));
    }

    @Test
    void initRecipe_ShouldNotIncludeRequestIdHeader_WhenRequestIdIsNotInMDC() {
        // Given
        Long recipeId = 1L;
        InitRecipeRequest request = new InitRecipeRequest(null);
        BranchDTO mockResponse = new BranchDTO();
        mockResponse.setId(1L);
        
        // No request ID in MDC
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(BranchDTO.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        BranchDTO result = versionClient.initRecipe(recipeId, request, testUserId);

        // Then
        assertNotNull(result);
        
        // Verify the request was made with correct headers
        ArgumentCaptor<HttpEntity<InitRecipeRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(vcsServiceUrl + "/vcs/recipes/" + recipeId + "/init"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(BranchDTO.class)
        );
        
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertEquals(testUserId.toString(), headers.getFirst("X-User-Id"));
        assertNull(headers.getFirst("X-Request-ID"));
    }

    @Test
    void copyRecipe_ShouldIncludeRequestIdHeader_WhenRequestIdIsInMDC() {
        // Given
        Long branchId = 1L;
        CopyBranchRequest request = new CopyBranchRequest(2L);
        BranchDTO mockResponse = new BranchDTO();
        mockResponse.setId(2L);
        
        // Set request ID in MDC
        MDC.put("requestId", testRequestId);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(BranchDTO.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        BranchDTO result = versionClient.copyRecipe(branchId, request, testUserId);

        // Then
        assertNotNull(result);
        
        // Verify the request was made with correct headers
        ArgumentCaptor<HttpEntity<CopyBranchRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(vcsServiceUrl + "/vcs/branches/" + branchId + "/copy"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(BranchDTO.class)
        );
        
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertEquals(testUserId.toString(), headers.getFirst("X-User-Id"));
        assertEquals(testRequestId, headers.getFirst("X-Request-ID"));
    }

    @Test
    void copyRecipe_ShouldNotIncludeRequestIdHeader_WhenRequestIdIsNotInMDC() {
        // Given
        Long branchId = 1L;
        CopyBranchRequest request = new CopyBranchRequest(2L);
        BranchDTO mockResponse = new BranchDTO();
        mockResponse.setId(2L);
        
        // No request ID in MDC
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(BranchDTO.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        BranchDTO result = versionClient.copyRecipe(branchId, request, testUserId);

        // Then
        assertNotNull(result);
        
        // Verify the request was made with correct headers
        ArgumentCaptor<HttpEntity<CopyBranchRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(vcsServiceUrl + "/vcs/branches/" + branchId + "/copy"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(BranchDTO.class)
        );
        
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertEquals(testUserId.toString(), headers.getFirst("X-User-Id"));
        assertNull(headers.getFirst("X-Request-ID"));
    }
} 