from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

class ChatResponse(BaseModel):
    """Chat response from AI"""
    reply: str
    sources: Optional[List[Dict[str, Any]]] = None
    recipe_suggestion: Optional[Dict[str, Any]] = None
    timestamp: datetime = Field(default_factory=datetime.now)

class RecipeIndexResponse(BaseModel):
    """Response for recipe indexing"""
    message: str
    recipe_id: int
    indexed_at: datetime = Field(default_factory=datetime.now)

class RecipeDeleteResponse(BaseModel):
    """Response for recipe deletion"""
    message: str
    recipe_id: int
    deleted_at: datetime = Field(default_factory=datetime.now)

class RecipeSuggestionResponse(BaseModel):
    """Response for recipe suggestion"""
    suggestion: str
    recipe_data: Dict[str, Any]
    timestamp: datetime = Field(default_factory=datetime.now)

class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    services: Dict[str, str]
    timestamp: datetime = Field(default_factory=datetime.now) 
