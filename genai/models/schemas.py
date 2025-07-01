from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

# Recipe DTOs matching the recipe microservice
class RecipeImageDTO(BaseModel):
    """Recipe image DTO matching the recipe service"""
    url: str

class RecipeIngredientDTO(BaseModel):
    """Recipe ingredient DTO matching the recipe service"""
    name: str
    unit: Optional[str] = None
    amount: Optional[float] = None

class RecipeStepDTO(BaseModel):
    """Recipe step DTO matching the recipe service"""
    order: int
    details: str
    recipeImageDTOS: Optional[List[RecipeImageDTO]] = []

class RecipeTagDTO(BaseModel):
    """Recipe tag DTO matching the recipe service"""
    id: Optional[int] = None
    name: str

class RecipeMetadataDTO(BaseModel):
    """Recipe metadata DTO matching the recipe service"""
    id: Optional[int] = None
    userId: int
    forkedFrom: Optional[int] = None
    createdAt: Optional[datetime] = None
    updatedAt: Optional[datetime] = None
    title: str
    description: Optional[str] = None
    thumbnail: Optional[RecipeImageDTO] = None
    servingSize: Optional[int] = None
    tags: List[RecipeTagDTO] = []

class RecipeDetailsDTO(BaseModel):
    """Recipe details DTO matching the recipe service"""
    servingSize: int
    images: Optional[List[RecipeImageDTO]] = []
    recipeIngredients: List[RecipeIngredientDTO]
    recipeSteps: List[RecipeStepDTO]

class InitRecipeRequest(BaseModel):
    """Recipe creation request matching the recipe service"""
    userId: int
    recipeDetails: RecipeDetailsDTO

# GenAI service specific models
class RecipeData(BaseModel):
    """Complete recipe data for vectorization"""
    metadata: RecipeMetadataDTO
    details: RecipeDetailsDTO

class ChatRequest(BaseModel):
    """Chat request from user"""
    message: str
    user_id: str

class ChatResponse(BaseModel):
    """Chat response from AI"""
    reply: str
    sources: Optional[List[Dict[str, Any]]] = None
    recipe_suggestion: Optional[Dict[str, Any]] = None
    timestamp: datetime = Field(default_factory=datetime.now)

class RecipeIndexRequest(BaseModel):
    """Request to index a recipe in vector store"""
    recipe: RecipeData

class RecipeIndexResponse(BaseModel):
    """Response for recipe indexing"""
    message: str
    recipe_id: int
    indexed_at: datetime = Field(default_factory=datetime.now)

class RecipeDeleteRequest(BaseModel):
    """Request to delete a recipe from vector store"""
    recipe_id: int

class RecipeDeleteResponse(BaseModel):
    """Response for recipe deletion"""
    message: str
    recipe_id: int
    deleted_at: datetime = Field(default_factory=datetime.now)

class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    services: Dict[str, str]
    timestamp: datetime = Field(default_factory=datetime.now) 