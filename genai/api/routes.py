from fastapi import APIRouter, HTTPException, BackgroundTasks, Depends
from typing import List, Dict, Any
from datetime import datetime
from loguru import logger

from models.schemas import (
    RecipeData,
    ChatRequest,
    ChatResponse,
    RecipeCreationRequest,
    RecipeCreationResponse,
    HealthResponse,
    IndexRecipeRequest,
    IndexRecipeResponse,
    RecipeMetadataDTO,
    RecipeDetailsDTO
)
from core.genai_service import GenAIService
from services.recipe_integration import RecipeServiceIntegration

# Initialize router
router = APIRouter(prefix="/api/v1")

# Initialize services
genai_service = GenAIService()
recipe_service = RecipeServiceIntegration()

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    try:
        services = {
            "genai_service": "healthy",
            "recipe_service": "healthy",
            "vector_store": "healthy"
        }
        
        # Basic health checks
        if not genai_service.llm:
            services["genai_service"] = "unhealthy"
        if not genai_service.vector_store:
            services["vector_store"] = "unhealthy"
        
        return HealthResponse(
            status="healthy" if all(v == "healthy" for v in services.values()) else "degraded",
            services=services
        )
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return HealthResponse(
            status="unhealthy",
            services={"error": str(e)}
        )

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Chat with the AI assistant for recipe search and creation"""
    try:
        response = await genai_service.chat(request)
        return response
    except Exception as e:
        logger.error(f"Error in chat endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/chat/history/{conversation_id}")
async def get_chat_history(conversation_id: str):
    """Get conversation history for a specific conversation"""
    try:
        history = genai_service.get_conversation_history(conversation_id)
        return {"conversation_id": conversation_id, "history": history}
    except Exception as e:
        logger.error(f"Error getting chat history: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/recipes/create", response_model=RecipeCreationResponse)
async def create_recipe(request: RecipeCreationRequest):
    """Create a new recipe using the LLM"""
    try:
        response = await genai_service.create_recipe(request)
        return response
    except Exception as e:
        logger.error(f"Error creating recipe: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/recipes/index", response_model=IndexRecipeResponse)
async def index_recipe(request: IndexRecipeRequest):
    """Index a recipe in the vector store"""
    try:
        success = await genai_service.index_recipe(request.recipe)
        if success:
            return IndexRecipeResponse(
                message="Recipe indexed successfully",
                recipe_id=request.recipe.id
            )
        else:
            raise HTTPException(status_code=500, detail="Failed to index recipe")
    except Exception as e:
        logger.error(f"Error indexing recipe: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/recipes/sync")
async def sync_recipes():
    """Sync all recipes from the recipe service to the vector store"""
    try:
        count = await genai_service.sync_recipes_from_service()
        return {"message": f"Synced {count} recipes to vector store", "count": count}
    except Exception as e:
        logger.error(f"Error syncing recipes: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/recipes/{recipe_id}")
async def get_recipe(recipe_id: int):
    """Get a recipe from the recipe service"""
    try:
        recipe = await recipe_service.get_recipe(recipe_id)
        if recipe:
            return recipe
        else:
            raise HTTPException(status_code=404, detail="Recipe not found")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error fetching recipe {recipe_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/recipes")
async def get_all_recipes(page: int = 0, size: int = 20):
    """Get all recipes from the recipe service"""
    try:
        recipes = await recipe_service.get_all_recipes(page=page, size=size)
        return {
            "content": [recipe.dict() for recipe in recipes],
            "page": page,
            "size": size,
            "total": len(recipes)
        }
    except Exception as e:
        logger.error(f"Error fetching recipes: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/recipes")
async def create_recipe_via_service(metadata: RecipeMetadataDTO, details: RecipeDetailsDTO):
    """Create a recipe directly in the recipe service"""
    try:
        created_recipe = await recipe_service.create_recipe(metadata, details)
        if created_recipe:
            return created_recipe
        else:
            raise HTTPException(status_code=500, detail="Failed to create recipe")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating recipe: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.put("/recipes/{recipe_id}")
async def update_recipe(recipe_id: int, metadata: RecipeMetadataDTO):
    """Update a recipe in the recipe service"""
    try:
        updated_recipe = await recipe_service.update_recipe(recipe_id, metadata)
        if updated_recipe:
            return updated_recipe
        else:
            raise HTTPException(status_code=404, detail="Recipe not found")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error updating recipe {recipe_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/recipes/{recipe_id}")
async def delete_recipe(recipe_id: int):
    """Delete a recipe from the recipe service"""
    try:
        success = await recipe_service.delete_recipe(recipe_id)
        if success:
            return {"message": "Recipe deleted successfully"}
        else:
            raise HTTPException(status_code=404, detail="Recipe not found")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting recipe {recipe_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/recipes/bulk-index")
async def bulk_index_recipes(recipes: List[RecipeData], background_tasks: BackgroundTasks):
    """Bulk index multiple recipes"""
    try:
        background_tasks.add_task(bulk_index_task, recipes)
        return {"message": f"Started indexing {len(recipes)} recipes in background"}
    except Exception as e:
        logger.error(f"Error starting bulk indexing: {e}")
        raise HTTPException(status_code=500, detail=str(e))

async def bulk_index_task(recipes: List[RecipeData]):
    """Background task for bulk indexing"""
    import asyncio
    
    for recipe in recipes:
        try:
            await genai_service.index_recipe(recipe)
            await asyncio.sleep(0.1)  # Small delay to prevent overwhelming
        except Exception as e:
            logger.error(f"Error indexing recipe {recipe.id}: {e}")

@router.get("/recipes/search")
async def search_recipes(query: str, limit: int = 5):
    """Search recipes using semantic search (for internal use)"""
    try:
        results = await genai_service.search_recipes_semantic(query, limit)
        return {"results": results, "query": query}
    except Exception as e:
        logger.error(f"Error searching recipes: {e}")
        raise HTTPException(status_code=500, detail=str(e)) 