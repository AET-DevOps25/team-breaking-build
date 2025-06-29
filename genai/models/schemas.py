from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

# Recipe data models matching the recipe microservice
class RecipeIngredientDTO(BaseModel):
    """Recipe ingredient DTO matching the recipe service"""
    name: str
    unit: Optional[str] = None
    amount: Optional[float] = None

class RecipeStepDTO(BaseModel):
    """Recipe step DTO matching the recipe service"""
    order: int
    details: str
    recipeImageDTOS: Optional[List[Dict[str, Any]]] = []

class RecipeTagDTO(BaseModel):
    """Recipe tag DTO matching the recipe service"""
    id: Optional[int] = None
    name: str

class RecipeImageDTO(BaseModel):
    """Recipe image DTO matching the recipe service"""
    url: str

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
    """Recipe data for indexing in vector store"""
    id: int
    title: str
    description: Optional[str] = None
    ingredients: List[RecipeIngredientDTO]
    steps: List[RecipeStepDTO]
    tags: List[str] = []
    serving_size: Optional[int] = None
    user_id: int

class ChatMessage(BaseModel):
    """Individual chat message"""
    role: str  # "user" or "assistant"
    content: str
    timestamp: datetime = Field(default_factory=datetime.now)

class ChatRequest(BaseModel):
    """Chat request from user"""
    message: str
    user_id: str
    conversation_id: Optional[str] = None
    context: Optional[str] = None

class ChatResponse(BaseModel):
    """Chat response from AI"""
    reply: str
    conversation_id: str
    sources: Optional[List[Dict[str, Any]]] = None
    recipe_created: Optional[Dict[str, Any]] = None
    timestamp: datetime = Field(default_factory=datetime.now)

class RecipeCreationRequest(BaseModel):
    """Request to create a new recipe"""
    description: str
    ingredients: List[str]
    dietary_restrictions: Optional[List[str]] = None
    cuisine_type: Optional[str] = None
    difficulty: Optional[str] = None
    serving_size: Optional[int] = None
    user_id: int

class RecipeCreationResponse(BaseModel):
    """Response with created recipe"""
    recipe: Dict[str, Any]
    created_at: datetime = Field(default_factory=datetime.now)

class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    services: Dict[str, str]
    timestamp: datetime = Field(default_factory=datetime.now)

class IndexRecipeRequest(BaseModel):
    """Request to index a recipe"""
    recipe: RecipeData

class IndexRecipeResponse(BaseModel):
    """Response for recipe indexing"""
    message: str
    recipe_id: int
    indexed_at: datetime = Field(default_factory=datetime.now) 