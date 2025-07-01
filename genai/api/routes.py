from fastapi import APIRouter, HTTPException, Depends

from core.genai_service import GenAIService
from models.schemas import (
    ChatRequest,
    ChatResponse,
    RecipeIndexRequest,
    RecipeIndexResponse,
    RecipeDeleteResponse,
    HealthResponse
)

router = APIRouter(prefix="/api/v1")

# Dependency to get GenAI service instance
def get_genai_service() -> GenAIService:
    return GenAIService()

@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    genai_service: GenAIService = Depends(get_genai_service)
) -> ChatResponse:
    """Chat with the AI assistant for recipe search and creation"""
    try:
        return genai_service.chat(request.message, request.user_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error in chat: {str(e)}")

@router.post("/recipes/index", response_model=RecipeIndexResponse)
async def index_recipe(
    request: RecipeIndexRequest,
    genai_service: GenAIService = Depends(get_genai_service)
) -> RecipeIndexResponse:
    """Index a recipe in the vector store"""
    try:
        success = genai_service.index_recipe(request.recipe)
        if success:
            return RecipeIndexResponse(
                message="Recipe indexed successfully",
                recipe_id=request.recipe.metadata.id
            )
        else:
            raise HTTPException(status_code=500, detail="Failed to index recipe")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error indexing recipe: {str(e)}")

@router.delete("/recipes/{recipe_id}", response_model=RecipeDeleteResponse)
async def delete_recipe(
    recipe_id: int,
    genai_service: GenAIService = Depends(get_genai_service)
) -> RecipeDeleteResponse:
    """Delete a recipe from the vector store"""
    try:
        success = genai_service.delete_recipe(recipe_id)
        if success:
            return RecipeDeleteResponse(
                message="Recipe deleted successfully",
                recipe_id=recipe_id
            )
        else:
            raise HTTPException(status_code=500, detail="Failed to delete recipe")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting recipe: {str(e)}")

@router.get("/health", response_model=HealthResponse)
async def health_check(
    genai_service: GenAIService = Depends(get_genai_service)
) -> HealthResponse:
    """Health check endpoint"""
    try:
        services_status = genai_service.get_health_status()
        return HealthResponse(
            status="healthy" if all("healthy" in status for status in services_status.values()) else "unhealthy",
            services=services_status
        )
    except Exception as e:
        return HealthResponse(
            status="unhealthy",
            services={"error": str(e)}
        ) 