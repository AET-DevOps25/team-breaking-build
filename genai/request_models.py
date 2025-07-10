from pydantic import BaseModel
from typing import List, Optional

# Recipe DTOs matching the recipe microservice
class RecipeIngredientDTO(BaseModel):
    name: str
    unit: Optional[str] = None
    amount: Optional[float] = None

class RecipeStepDTO(BaseModel):
    order: int
    details: str

class RecipeTagDTO(BaseModel):
    id: Optional[int] = None
    name: str

class RecipeMetadataDTO(BaseModel):
    id: Optional[int] = None
    title: str
    description: Optional[str] = None
    servingSize: Optional[int] = None
    tags: List[RecipeTagDTO] = []

class RecipeDetailsDTO(BaseModel):
    servingSize: int
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
