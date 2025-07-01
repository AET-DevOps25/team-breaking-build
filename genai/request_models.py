from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

# Recipe DTOs matching the recipe microservice
class RecipeImageDTO(BaseModel):
    url: str

class RecipeIngredientDTO(BaseModel):
    name: str
    unit: Optional[str] = None
    amount: Optional[float] = None

class RecipeStepDTO(BaseModel):
    order: int
    details: str
    recipeImageDTOS: Optional[List[RecipeImageDTO]] = []

class RecipeTagDTO(BaseModel):
    id: Optional[int] = None
    name: str

class RecipeMetadataDTO(BaseModel):
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
    servingSize: int
    images: Optional[List[RecipeImageDTO]] = []
    recipeIngredients: List[RecipeIngredientDTO]
    recipeSteps: List[RecipeStepDTO]

class RecipeData(BaseModel):
    """Complete recipe data for vectorization"""
    metadata: RecipeMetadataDTO
    details: RecipeDetailsDTO

# GenAI service request models
class ChatRequest(BaseModel):
    """Chat request from user"""
    message: str

class RecipeIndexRequest(BaseModel):
    """Request to index a recipe in vector store"""
    recipe: RecipeData

class RecipeDeleteRequest(BaseModel):
    """Request to delete a recipe from vector store"""
    recipe_id: int

class RecipeSuggestionRequest(BaseModel):
    """Request for recipe suggestion"""
    query: str 
