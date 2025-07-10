from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import logging

from request_models import ChatRequest, RecipeIndexRequest, RecipeDeleteRequest, RecipeSuggestionRequest
from response_models import ChatResponse, RecipeIndexResponse, RecipeDeleteResponse, RecipeSuggestionResponse, HealthResponse
from llm import RecipeLLM

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)

# Global LLM instance
llm_instance: RecipeLLM = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: initialize LLM
    global llm_instance
    try:
        llm_instance = RecipeLLM()
        logger.info("GenAI service started successfully")
    except Exception as e:
        logger.error(f"Failed to start GenAI service: {e}")
        raise
    
    yield
    
    # Shutdown: cleanup
    if llm_instance:
        llm_instance.cleanup()
        logger.info("GenAI service shutdown completed")

app = FastAPI(
    title="GenAI Recipe Service",
    version="1.0.0",
    description="GenAI Recipe Service - Vector operations and LLM interactions for recipes",
    lifespan=lifespan
)

@app.get("/genai")
async def root():
    """Root endpoint"""
    return {
        "message": "GenAI Recipe Service",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    try:
        if not llm_instance:
            return HealthResponse(
                status="unhealthy",
                services={"error": "LLM instance not initialized"}
            )
        
        services_status = llm_instance.get_health_status()
        return HealthResponse(
            status="healthy" if all("healthy" in status for status in services_status.values()) else "unhealthy",
            services=services_status
        )
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return HealthResponse(
            status="unhealthy",
            services={"error": str(e)}
        )

@app.post("/genai/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Chat with the AI assistant for recipe search and creation"""
    try:
        if not llm_instance:
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        return llm_instance.chat(request.message)
    except Exception as e:
        logger.error(f"Error in chat: {e}")
        raise HTTPException(status_code=500, detail=f"Error in chat: {str(e)}")

@app.post("/genai/vector/index", response_model=RecipeIndexResponse)
async def index_recipe(request: RecipeIndexRequest):
    """Index a recipe in the vector store"""
    try:
        if not llm_instance:
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        success = llm_instance.index_recipe(request.recipe)
        if success:
            return RecipeIndexResponse(
                message="Recipe indexed successfully",
                recipe_id=request.recipe.metadata.id
            )
        else:
            raise HTTPException(status_code=500, detail="Failed to index recipe")
    except Exception as e:
        logger.error(f"Error indexing recipe: {e}")
        raise HTTPException(status_code=500, detail=f"Error indexing recipe: {str(e)}")

@app.delete("/genai/vector/{recipe_id}", response_model=RecipeDeleteResponse)
async def delete_recipe(recipe_id: str):
    """Delete a recipe from the vector store"""
    try:
        if not llm_instance:
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        success = llm_instance.delete_recipe(recipe_id)
        if success:
            return RecipeDeleteResponse(
                message="Recipe deleted successfully",
                recipe_id=recipe_id
            )
        else:
            raise HTTPException(status_code=500, detail="Failed to delete recipe")
    except Exception as e:
        logger.error(f"Error deleting recipe: {e}")
        raise HTTPException(status_code=500, detail=f"Error deleting recipe: {str(e)}")

@app.post("/genai/vector/suggest", response_model=RecipeSuggestionResponse)
async def suggest_recipe(request: RecipeSuggestionRequest):
    """Generate a recipe suggestion based on query and similar recipes"""
    try:
        if not llm_instance:
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        return llm_instance.suggest_recipe(request.query)
    except Exception as e:
        logger.error(f"Error in recipe suggestion: {e}")
        raise HTTPException(status_code=500, detail=f"Error in recipe suggestion: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8080,
        reload=False
    ) 
