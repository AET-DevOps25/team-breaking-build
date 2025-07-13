import os
import json
import time
import uuid
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.middleware.cors import CORSMiddleware
import logging
from typing import Callable

# Prometheus instrumentator import
from prometheus_fastapi_instrumentator import Instrumentator

from request_models import ChatRequest, RecipeIndexRequest, RecipeDeleteRequest, RecipeSuggestionRequest
from response_models import ChatResponse, RecipeIndexResponse, RecipeDeleteResponse, RecipeSuggestionResponse, HealthResponse
from llm import RecipeLLM

# Enhanced logging configuration
class StructuredFormatter(logging.Formatter):
    """Custom formatter for structured logging"""
    
    def format(self, record):
        log_entry = {
            'timestamp': self.formatTime(record, self.datefmt),
            'level': record.levelname,
            'service': 'genai-service',
            'logger': record.name,
            'message': record.getMessage(),
            'module': record.module,
            'function': record.funcName,
            'line': record.lineno
        }
        
        # Add request ID if available
        if hasattr(record, 'request_id'):
            log_entry['request_id'] = record.request_id
            
        # Add timing information if available
        if hasattr(record, 'duration_ms'):
            log_entry['duration_ms'] = record.duration_ms
            
        # Add extra context if available
        if hasattr(record, 'extra_context'):
            log_entry.update(record.extra_context)
            
        return json.dumps(log_entry)

# Configure logging with enhanced structured format
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler()
    ]
)

# Create structured logger for JSON output
structured_logger = logging.getLogger("structured")
structured_handler = logging.StreamHandler()
structured_handler.setFormatter(StructuredFormatter())
structured_logger.addHandler(structured_handler)
structured_logger.setLevel(logging.INFO)

logger = logging.getLogger(__name__)

# Add request ID tracking middleware
async def add_request_id_middleware(request: Request, call_next: Callable) -> Response:
    """Add request ID to all requests for tracing"""
    request_id = str(uuid.uuid4())
    request.state.request_id = request_id
    
    # Log incoming request
    start_time = time.time()
    structured_logger.info(
        f"Incoming request: {request.method} {request.url.path}",
        extra={
            'request_id': request_id,
            'extra_context': {
                'method': request.method,
                'path': request.url.path,
                'query_params': str(request.query_params),
                'client_ip': request.client.host if request.client else None,
                'user_agent': request.headers.get('user-agent', 'unknown')
            }
        }
    )
    
    try:
        response = await call_next(request)
        
        # Calculate request duration
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        # Log response
        structured_logger.info(
            f"Request completed: {request.method} {request.url.path} - {response.status_code}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'status_code': response.status_code,
                    'response_time_ms': duration_ms
                }
            }
        )
        
        # Add request ID to response headers
        response.headers["X-Request-ID"] = request_id
        
        return response
        
    except Exception as e:
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        structured_logger.error(
            f"Request failed: {request.method} {request.url.path} - {str(e)}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'error': str(e),
                    'error_type': type(e).__name__
                }
            }
        )
        raise

# Global LLM instance
llm_instance: RecipeLLM = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: initialize LLM
    global llm_instance
    try:
        logger.info("Starting GenAI service initialization...")
        structured_logger.info("GenAI service startup initiated", extra={'extra_context': {'phase': 'startup'}})
        
        llm_instance = RecipeLLM()
        
        logger.info("GenAI service started successfully")
        structured_logger.info("GenAI service initialization completed", extra={'extra_context': {'phase': 'startup', 'status': 'success'}})
        
    except Exception as e:
        logger.error(f"Failed to start GenAI service: {e}", exc_info=True)
        structured_logger.error(
            f"GenAI service startup failed: {str(e)}",
            extra={'extra_context': {'phase': 'startup', 'status': 'failed', 'error': str(e), 'error_type': type(e).__name__}}
        )
        raise
    
    yield
    
    # Shutdown: cleanup
    structured_logger.info("GenAI service shutdown initiated", extra={'extra_context': {'phase': 'shutdown'}})
    
    if llm_instance:
        try:
            llm_instance.cleanup()
            logger.info("GenAI service shutdown completed")
            structured_logger.info("GenAI service shutdown completed", extra={'extra_context': {'phase': 'shutdown', 'status': 'success'}})
        except Exception as e:
            logger.error(f"Error during GenAI service shutdown: {e}", exc_info=True)
            structured_logger.error(
                f"GenAI service shutdown failed: {str(e)}",
                extra={'extra_context': {'phase': 'shutdown', 'status': 'failed', 'error': str(e)}}
            )

app = FastAPI(
    title="GenAI Recipe Service",
    version="1.0.0",
    description="GenAI Recipe Service - Vector operations and LLM interactions for recipes",
    lifespan=lifespan
)

# Add Prometheus metrics
Instrumentator().instrument(app).expose(app)

# Add middleware
app.middleware("http")(add_request_id_middleware)

@app.get("/genai")
async def root(request: Request):
    """Root endpoint"""
    request_id = getattr(request.state, 'request_id', 'unknown')
    
    structured_logger.info(
        "Root endpoint accessed",
        extra={
            'request_id': request_id,
            'extra_context': {'endpoint': 'root'}
        }
    )
    
    return {
        "message": "GenAI Recipe Service",
        "version": "1.0.0",
        "status": "running",
        "request_id": request_id
    }

@app.get("/health", response_model=HealthResponse)
async def health_check(request: Request):
    """Health check endpoint"""
    request_id = getattr(request.state, 'request_id', 'unknown')
    start_time = time.time()
    
    structured_logger.info(
        "Health check initiated",
        extra={
            'request_id': request_id,
            'extra_context': {'endpoint': 'health'}
        }
    )
    
    try:
        if not llm_instance:
            structured_logger.error(
                "Health check failed - LLM instance not initialized",
                extra={
                    'request_id': request_id,
                    'extra_context': {'error': 'llm_not_initialized'}
                }
            )
            
            return HealthResponse(
                status="unhealthy",
                services={"error": "LLM instance not initialized"}
            )
        
        services_status = llm_instance.get_health_status()
        overall_status = "healthy" if all("healthy" in status for status in services_status.values()) else "unhealthy"
        
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        structured_logger.info(
            f"Health check completed - {overall_status}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'overall_status': overall_status,
                    'services_status': services_status
                }
            }
        )
        
        return HealthResponse(
            status=overall_status,
            services=services_status
        )
        
    except Exception as e:
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        logger.error(f"Health check failed: {e}", exc_info=True)
        structured_logger.error(
            f"Health check failed: {str(e)}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'error': str(e),
                    'error_type': type(e).__name__
                }
            }
        )
        
        return HealthResponse(
            status="unhealthy",
            services={"error": str(e)}
        )

@app.post("/genai/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, http_request: Request):
    """Chat with the AI assistant for recipe search and creation"""
    request_id = getattr(http_request.state, 'request_id', 'unknown')
    start_time = time.time()
    
    structured_logger.info(
        f"Chat request received: {request.message[:100]}...",
        extra={
            'request_id': request_id,
            'extra_context': {
                'endpoint': 'chat',
                'message_length': len(request.message),
                'message_preview': request.message[:100]
            }
        }
    )
    
    try:
        if not llm_instance:
            structured_logger.error(
                "Chat request failed - LLM service not initialized",
                extra={
                    'request_id': request_id,
                    'extra_context': {'error': 'llm_not_initialized'}
                }
            )
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        response = llm_instance.chat(request.message)
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        structured_logger.info(
            "Chat request completed successfully",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'response_type': 'chat',
                    'has_sources': response.sources is not None,
                    'has_recipe_suggestion': response.recipe_suggestion is not None,
                    'response_length': len(response.reply) if response.reply else 0
                }
            }
        )
        
        return response
        
    except Exception as e:
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        logger.error(f"Error in chat: {e}", exc_info=True)
        structured_logger.error(
            f"Chat request failed: {str(e)}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'error': str(e),
                    'error_type': type(e).__name__,
                    'message_length': len(request.message)
                }
            }
        )
        
        raise HTTPException(status_code=500, detail=f"Error in chat: {str(e)}")

@app.post("/genai/vector/index", response_model=RecipeIndexResponse)
async def index_recipe(request: RecipeIndexRequest, http_request: Request):
    """Index a recipe in the vector store"""
    request_id = getattr(http_request.state, 'request_id', 'unknown')
    start_time = time.time()
    
    structured_logger.info(
        f"Recipe indexing request received: {request.recipe.metadata.title}",
        extra={
            'request_id': request_id,
            'extra_context': {
                'endpoint': 'index_recipe',
                'recipe_id': request.recipe.metadata.id,
                'recipe_title': request.recipe.metadata.title,
                'ingredient_count': len(request.recipe.details.recipeIngredients),
                'step_count': len(request.recipe.details.recipeSteps)
            }
        }
    )
    
    try:
        if not llm_instance:
            structured_logger.error(
                "Recipe indexing failed - LLM service not initialized",
                extra={
                    'request_id': request_id,
                    'extra_context': {'error': 'llm_not_initialized'}
                }
            )
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        success = llm_instance.index_recipe(request.recipe)
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        if success:
            structured_logger.info(
                f"Recipe indexed successfully: {request.recipe.metadata.title}",
                extra={
                    'request_id': request_id,
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'recipe_id': request.recipe.metadata.id,
                        'recipe_title': request.recipe.metadata.title,
                        'status': 'success'
                    }
                }
            )
            
            return RecipeIndexResponse(
                message="Recipe indexed successfully",
                recipe_id=request.recipe.metadata.id
            )
        else:
            structured_logger.error(
                f"Recipe indexing failed: {request.recipe.metadata.title}",
                extra={
                    'request_id': request_id,
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'recipe_id': request.recipe.metadata.id,
                        'recipe_title': request.recipe.metadata.title,
                        'status': 'failed'
                    }
                }
            )
            
            raise HTTPException(status_code=500, detail="Failed to index recipe")
            
    except Exception as e:
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        logger.error(f"Error indexing recipe: {e}", exc_info=True)
        structured_logger.error(
            f"Recipe indexing failed: {str(e)}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'error': str(e),
                    'error_type': type(e).__name__,
                    'recipe_id': request.recipe.metadata.id,
                    'recipe_title': request.recipe.metadata.title
                }
            }
        )
        
        raise HTTPException(status_code=500, detail=f"Error indexing recipe: {str(e)}")

@app.delete("/genai/vector/{recipe_id}", response_model=RecipeDeleteResponse)
async def delete_recipe(recipe_id: str, http_request: Request):
    """Delete a recipe from the vector store"""
    request_id = getattr(http_request.state, 'request_id', 'unknown')
    start_time = time.time()
    
    structured_logger.info(
        f"Recipe deletion request received: {recipe_id}",
        extra={
            'request_id': request_id,
            'extra_context': {
                'endpoint': 'delete_recipe',
                'recipe_id': recipe_id
            }
        }
    )
    
    try:
        if not llm_instance:
            structured_logger.error(
                "Recipe deletion failed - LLM service not initialized",
                extra={
                    'request_id': request_id,
                    'extra_context': {'error': 'llm_not_initialized'}
                }
            )
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        success = llm_instance.delete_recipe(recipe_id)
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        if success:
            structured_logger.info(
                f"Recipe deleted successfully: {recipe_id}",
                extra={
                    'request_id': request_id,
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'recipe_id': recipe_id,
                        'status': 'success'
                    }
                }
            )
            
            return RecipeDeleteResponse(
                message="Recipe deleted successfully",
                recipe_id=recipe_id
            )
        else:
            structured_logger.error(
                f"Recipe deletion failed: {recipe_id}",
                extra={
                    'request_id': request_id,
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'recipe_id': recipe_id,
                        'status': 'failed'
                    }
                }
            )
            
            raise HTTPException(status_code=500, detail="Failed to delete recipe")
            
    except Exception as e:
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        logger.error(f"Error deleting recipe: {e}", exc_info=True)
        structured_logger.error(
            f"Recipe deletion failed: {str(e)}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'error': str(e),
                    'error_type': type(e).__name__,
                    'recipe_id': recipe_id
                }
            }
        )
        
        raise HTTPException(status_code=500, detail=f"Error deleting recipe: {str(e)}")

@app.post("/genai/vector/suggest", response_model=RecipeSuggestionResponse)
async def suggest_recipe(request: RecipeSuggestionRequest, http_request: Request):
    """Generate a recipe suggestion based on query and similar recipes"""
    request_id = getattr(http_request.state, 'request_id', 'unknown')
    start_time = time.time()
    
    structured_logger.info(
        f"Recipe suggestion request received: {request.query[:100]}...",
        extra={
            'request_id': request_id,
            'extra_context': {
                'endpoint': 'suggest_recipe',
                'query_length': len(request.query),
                'query_preview': request.query[:100]
            }
        }
    )
    
    try:
        if not llm_instance:
            structured_logger.error(
                "Recipe suggestion failed - LLM service not initialized",
                extra={
                    'request_id': request_id,
                    'extra_context': {'error': 'llm_not_initialized'}
                }
            )
            raise HTTPException(status_code=500, detail="LLM service not initialized")
        
        response = llm_instance.suggest_recipe(request.query)
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        structured_logger.info(
            "Recipe suggestion completed successfully",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'query_length': len(request.query),
                    'has_recipe_data': bool(response.recipe_data),
                    'suggestion_length': len(response.suggestion) if response.suggestion else 0
                }
            }
        )
        
        return response
        
    except Exception as e:
        duration_ms = round((time.time() - start_time) * 1000, 2)
        
        logger.error(f"Error in recipe suggestion: {e}", exc_info=True)
        structured_logger.error(
            f"Recipe suggestion failed: {str(e)}",
            extra={
                'request_id': request_id,
                'duration_ms': duration_ms,
                'extra_context': {
                    'error': str(e),
                    'error_type': type(e).__name__,
                    'query_length': len(request.query)
                }
            }
        )
        
        raise HTTPException(status_code=500, detail=f"Error in recipe suggestion: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    
    structured_logger.info(
        "Starting GenAI service server",
        extra={'extra_context': {'host': '0.0.0.0', 'port': 8080, 'workers': 4}}
    )
    
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8080,
        reload=False,
        workers=4
    ) 
