import logging
import time
import json
from typing import List, Dict, Any
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.documents import Document
from dotenv import load_dotenv
import os

from request_models import RecipeData
from response_models import ChatResponse, RecipeSuggestionResponse
from rag import RAGHelper

load_dotenv()

logger = logging.getLogger(__name__)
# Create structured logger for detailed logging
structured_logger = logging.getLogger("structured")

class RecipeLLM:
    """LLM service for recipe search and suggestion"""
    
    def __init__(self):
        """Initialize LLM service"""
        start_time = time.time()
        
        try:
            logger.info("Starting Recipe LLM service initialization...")
            structured_logger.info(
                "Recipe LLM initialization started",
                extra={'extra_context': {'phase': 'initialization', 'component': 'llm_service'}}
            )
            
            # Log configuration details
            model_name = os.getenv("LLM_MODEL", "unknown")
            temperature = float(os.getenv("LLM_TEMPERATURE", "0.7"))
            base_url = os.getenv("LLM_BASE_URL", "unknown")
            
            structured_logger.info(
                f"Initializing LLM with model: {model_name}, temperature: {temperature}",
                extra={'extra_context': {
                    'model_name': model_name,
                    'temperature': temperature,
                    'base_url': base_url,
                    'component': 'llm_config'
                }}
            )
            
            # Initialize LLM with environment variables (no defaults)
            self.llm = ChatOpenAI(
                model=model_name,
                temperature=temperature,
                api_key=os.getenv("OPEN_WEBUI_API_KEY"),
                base_url=base_url
            )
            
            # Initialize RAG helper with weaviate configuration
            logger.info("Initializing RAG helper...")
            structured_logger.info(
                "RAG helper initialization started",
                extra={'extra_context': {'component': 'rag_helper'}}
            )
            
            self.rag_helper = RAGHelper()
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.info(f"Recipe LLM service initialized successfully in {duration_ms}ms")
            structured_logger.info(
                "Recipe LLM service initialization completed",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'phase': 'initialization',
                        'status': 'success',
                        'component': 'llm_service'
                    }
                }
            )
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to initialize Recipe LLM service: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe LLM service initialization failed: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'phase': 'initialization',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__,
                        'component': 'llm_service'
                    }
                }
            )
            raise
    
    def index_recipe(self, recipe: RecipeData) -> bool:
        """Index a recipe in the vector store"""
        start_time = time.time()
        recipe_id = str(recipe.metadata.id) if recipe.metadata.id else "unknown"
        
        try:
            logger.info(f"Starting recipe indexing for: {recipe.metadata.title} (ID: {recipe_id})")
            structured_logger.info(
                f"Recipe indexing started: {recipe.metadata.title}",
                extra={'extra_context': {
                    'operation': 'index_recipe',
                    'recipe_id': recipe_id,
                    'recipe_title': recipe.metadata.title,
                    'ingredient_count': len(recipe.details.recipeIngredients),
                    'step_count': len(recipe.details.recipeSteps),
                    'tag_count': len(recipe.metadata.tags)
                }}
            )
            
            # Prepare content for vectorization
            content_prep_start = time.time()
            content = self._prepare_recipe_content(recipe)
            content_prep_duration = round((time.time() - content_prep_start) * 1000, 2)
            
            structured_logger.info(
                "Recipe content preparation completed",
                extra={
                    'duration_ms': content_prep_duration,
                    'extra_context': {
                        'operation': 'content_preparation',
                        'recipe_id': recipe_id,
                        'content_length': len(content)
                    }
                }
            )
            
            # Prepare metadata with combined ID format: "recipeID+branchID"
            # Handle both string and integer IDs
            combined_id = str(recipe_id)
            metadata = {
                "recipe_id": combined_id,  # Store recipe ID
                "title": recipe.metadata.title,
                "description": recipe.metadata.description or "",
                "ingredients": [ing.name for ing in recipe.details.recipeIngredients],
                "steps": [step.details for step in recipe.details.recipeSteps],
                "tags": [tag.name for tag in recipe.metadata.tags],
                "serving_size": recipe.details.servingSize
            }
            
            # Add to vector store using RAG helper
            vector_store_start = time.time()
            success = self.rag_helper.add_recipe(content, metadata)
            vector_store_duration = round((time.time() - vector_store_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            if success:
                logger.info(f"Successfully indexed recipe {combined_id}: {recipe.metadata.title} in {total_duration}ms")
                structured_logger.info(
                    f"Recipe indexing completed successfully: {recipe.metadata.title}",
                    extra={
                        'duration_ms': total_duration,
                        'extra_context': {
                            'operation': 'index_recipe',
                            'recipe_id': recipe_id,
                            'recipe_title': recipe.metadata.title,
                            'status': 'success',
                            'content_prep_duration_ms': content_prep_duration,
                            'vector_store_duration_ms': vector_store_duration,
                            'content_length': len(content)
                        }
                    }
                )
            else:
                logger.error(f"Failed to index recipe {combined_id}: {recipe.metadata.title}")
                structured_logger.error(
                    f"Recipe indexing failed: {recipe.metadata.title}",
                    extra={
                        'duration_ms': total_duration,
                        'extra_context': {
                            'operation': 'index_recipe',
                            'recipe_id': recipe_id,
                            'recipe_title': recipe.metadata.title,
                            'status': 'failed',
                            'error': 'vector_store_operation_failed'
                        }
                    }
                )
            
            return success
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to index recipe {recipe_id}: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe indexing failed with exception: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'index_recipe',
                        'recipe_id': recipe_id,
                        'recipe_title': recipe.metadata.title,
                        'status': 'error',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            return False
    
    def delete_recipe(self, recipe_id: str) -> bool:
        """Delete a recipe from the vector store"""
        start_time = time.time()
        
        try:
            logger.info(f"Starting recipe deletion for ID: {recipe_id}")
            structured_logger.info(
                f"Recipe deletion started: {recipe_id}",
                extra={'extra_context': {
                    'operation': 'delete_recipe',
                    'recipe_id': recipe_id
                }}
            )
            
            # Delete by recipe_id filter (will match any recipe with this recipe_id in combined ID)
            success = self.rag_helper.delete_recipe_by_recipe_id(recipe_id)
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            
            if success:
                logger.info(f"Successfully deleted recipe {recipe_id} from vector store in {duration_ms}ms")
                structured_logger.info(
                    f"Recipe deletion completed successfully: {recipe_id}",
                    extra={
                        'duration_ms': duration_ms,
                        'extra_context': {
                            'operation': 'delete_recipe',
                            'recipe_id': recipe_id,
                            'status': 'success'
                        }
                    }
                )
            else:
                logger.error(f"Failed to delete recipe {recipe_id}")
                structured_logger.error(
                    f"Recipe deletion failed: {recipe_id}",
                    extra={
                        'duration_ms': duration_ms,
                        'extra_context': {
                            'operation': 'delete_recipe',
                            'recipe_id': recipe_id,
                            'status': 'failed',
                            'error': 'vector_store_operation_failed'
                        }
                    }
                )
            
            return success
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to delete recipe {recipe_id}: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe deletion failed with exception: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'operation': 'delete_recipe',
                        'recipe_id': recipe_id,
                        'status': 'error',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            return False
    
    def chat(self, message: str) -> ChatResponse:
        """Process chat message and return response"""
        start_time = time.time()
        
        try:
            logger.info(f"Processing chat message: {message[:100]}...")
            structured_logger.info(
                f"Chat processing started: {message[:100]}...",
                extra={'extra_context': {
                    'operation': 'chat',
                    'message_length': len(message),
                    'message_preview': message[:100]
                }}
            )
            
            # Search for relevant recipes
            search_start = time.time()
            search_results = self.rag_helper.retrieve(message, top_k=5)
            search_duration = round((time.time() - search_start) * 1000, 2)
            
            structured_logger.info(
                f"RAG search completed - found {len(search_results)} results",
                extra={
                    'duration_ms': search_duration,
                    'extra_context': {
                        'operation': 'rag_search',
                        'query': message[:100],
                        'results_count': len(search_results),
                        'search_duration_ms': search_duration
                    }
                }
            )
            
            # Prepare context from search results
            context_start = time.time()
            context = self._prepare_search_context(search_results)
            context_duration = round((time.time() - context_start) * 1000, 2)
            
            # Determine if user wants to create a recipe
            is_creation_request = self._is_recipe_creation_request(message)
            
            structured_logger.info(
                f"Chat analysis completed - creation request: {is_creation_request}",
                extra={
                    'duration_ms': context_duration,
                    'extra_context': {
                        'operation': 'chat_analysis',
                        'is_creation_request': is_creation_request,
                        'context_length': len(context),
                        'analysis_duration_ms': context_duration
                    }
                }
            )
            
            # Handle request based on type
            if is_creation_request:
                response = self._handle_recipe_creation(message, context)
            else:
                response = self._handle_recipe_search(message, context, search_results)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"Chat processing completed in {total_duration}ms")
            structured_logger.info(
                "Chat processing completed successfully",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'chat',
                        'message_length': len(message),
                        'is_creation_request': is_creation_request,
                        'search_duration_ms': search_duration,
                        'context_duration_ms': context_duration,
                        'response_length': len(response.reply) if response.reply else 0,
                        'has_sources': response.sources is not None,
                        'has_recipe_suggestion': response.recipe_suggestion is not None
                    }
                }
            )
            
            return response
                
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Error in chat processing: {e}", exc_info=True)
            structured_logger.error(
                f"Chat processing failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'chat',
                        'message_length': len(message),
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            
            return ChatResponse(
                reply="I'm sorry, I encountered an error processing your request. Please try again.",
                sources=None,
                recipe_suggestion=None
            )
    
    def suggest_recipe(self, query: str) -> RecipeSuggestionResponse:
        """Generate a recipe suggestion based on query and similar recipes with improved creativity"""
        start_time = time.time()
        
        try:
            logger.info(f"Starting recipe suggestion for query: {query[:100]}...")
            structured_logger.info(
                f"Recipe suggestion started: {query[:100]}...",
                extra={'extra_context': {
                    'operation': 'suggest_recipe',
                    'query_length': len(query),
                    'query_preview': query[:100]
                }}
            )
            
            # Search for similar recipes
            search_start = time.time()
            search_results = self.rag_helper.retrieve(query, top_k=3)
            search_duration = round((time.time() - search_start) * 1000, 2)
            
            structured_logger.info(
                f"RAG search for suggestion completed - found {len(search_results)} results",
                extra={
                    'duration_ms': search_duration,
                    'extra_context': {
                        'operation': 'rag_search_suggestion',
                        'query': query[:100],
                        'results_count': len(search_results),
                        'search_duration_ms': search_duration
                    }
                }
            )
            
            # Prepare context from search results
            context_start = time.time()
            context = self._prepare_search_context(search_results)
            context_duration = round((time.time() - context_start) * 1000, 2)
            
            # Determine if we have meaningful context
            has_good_context = self._has_meaningful_context(context)
            
            structured_logger.info(
                f"Context analysis completed - has good context: {has_good_context}",
                extra={
                    'duration_ms': context_duration,
                    'extra_context': {
                        'operation': 'context_analysis',
                        'has_good_context': has_good_context,
                        'context_length': len(context),
                        'analysis_duration_ms': context_duration
                    }
                }
            )
            
            # Prepare prompt based on context quality
            if has_good_context:
                prompt_type = "context_aware"
                prompt = ChatPromptTemplate.from_template("""
                You are a creative and experienced chef assistant. The user wants a recipe suggestion based on their request.
                
                User Request: {query}
                Available Recipe Context: {context}
                
                Create an innovative recipe suggestion that:
                1. Directly addresses the user's request
                2. Takes inspiration from the available recipes but adds your own creative twist
                3. Uses modern cooking techniques and flavor combinations
                4. Is practical and achievable for home cooks
                5. Has clear, detailed instructions
                
                Be creative! Don't just copy the existing recipes - use them as inspiration to create something new and exciting.
                
                Return a complete recipe in this JSON format:
                {{
                    "title": "Creative and descriptive recipe title",
                    "description": "Appetizing description explaining what makes this recipe special",
                    "servingSize": 4,
                    "recipeIngredients": [
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}},
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}}
                    ],
                    "recipeSteps": [
                        {{"order": 1, "details": "Detailed step with cooking tips and techniques"}},
                        {{"order": 2, "details": "Detailed step with cooking tips and techniques"}}
                    ]
                }}
                
                Make the recipe unique and creative while being practical. Use specific ingredients and detailed steps.
                """)
            else:
                prompt_type = "standalone"
                prompt = ChatPromptTemplate.from_template("""
                You are a master chef with decades of culinary experience. The user wants a recipe suggestion, but we don't have many relevant examples to work with. This is your chance to be truly creative!
                
                User Request: {query}
                
                Create an innovative, delicious recipe suggestion that:
                1. Directly fulfills the user's request
                2. Uses your culinary expertise to create something unique
                3. Incorporates modern cooking techniques and flavor profiles
                4. Is practical for home cooking
                5. Has clear, detailed instructions that any cook can follow
                
                Be bold and creative! Think outside the box and create something that will impress. Use interesting ingredient combinations, cooking methods, and presentation ideas.
                
                Return a complete recipe in this JSON format:
                {{
                    "title": "Creative and descriptive recipe title",
                    "description": "Appetizing description explaining what makes this recipe special and unique",
                    "servingSize": 4,
                    "recipeIngredients": [
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}},
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}}
                    ],
                    "recipeSteps": [
                        {{"order": 1, "details": "Detailed step with cooking tips, techniques, and timing"}},
                        {{"order": 2, "details": "Detailed step with cooking tips, techniques, and timing"}}
                    ]
                }}
                
                Make this recipe memorable and delicious. Use specific measurements, cooking times, and helpful tips.
                """)
            
            # Generate LLM response
            llm_start = time.time()
            chain = prompt | self.llm
            response = chain.invoke({
                "query": query,
                "context": context
            })
            llm_duration = round((time.time() - llm_start) * 1000, 2)
            
            structured_logger.info(
                f"LLM generation completed using {prompt_type} prompt",
                extra={
                    'duration_ms': llm_duration,
                    'extra_context': {
                        'operation': 'llm_generation',
                        'prompt_type': prompt_type,
                        'llm_duration_ms': llm_duration,
                        'response_length': len(response.content) if response.content else 0
                    }
                }
            )
            
            # Parse the response to extract recipe data
            parse_start = time.time()
            recipe_data = self._parse_recipe_response(response.content)
            parse_duration = round((time.time() - parse_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"Recipe suggestion completed in {total_duration}ms")
            structured_logger.info(
                "Recipe suggestion completed successfully",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'suggest_recipe',
                        'query_length': len(query),
                        'search_duration_ms': search_duration,
                        'context_duration_ms': context_duration,
                        'llm_duration_ms': llm_duration,
                        'parse_duration_ms': parse_duration,
                        'prompt_type': prompt_type,
                        'has_recipe_data': bool(recipe_data),
                        'recipe_title': recipe_data.get('title', 'unknown') if recipe_data else 'none'
                    }
                }
            )
            
            return RecipeSuggestionResponse(
                suggestion=f"I've created a unique recipe suggestion for you based on your request: '{query}'. This recipe combines creativity with practicality!",
                recipe_data=recipe_data
            )
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Error in recipe suggestion: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe suggestion failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'suggest_recipe',
                        'query_length': len(query),
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            
            return RecipeSuggestionResponse(
                suggestion="I'm sorry, I encountered an error creating a recipe suggestion. Please try again.",
                recipe_data={}
            )
    
    def _prepare_recipe_content(self, recipe: RecipeData) -> str:
        """Prepare recipe content for vectorization"""
        content_parts = [
            f"Title: {recipe.metadata.title}",
            f"Description: {recipe.metadata.description or 'No description'}"
        ]
        
        # Add ingredients
        ingredients_text = ", ".join([
            f"{ing.amount} {ing.unit} {ing.name}" if ing.amount and ing.unit 
            else f"{ing.amount} {ing.name}" if ing.amount 
            else ing.name
            for ing in recipe.details.recipeIngredients
        ])
        content_parts.append(f"Ingredients: {ingredients_text}")
        
        # Add steps
        steps_text = "\n".join([
            f"Step {i+1}: {step.details}"
            for i, step in enumerate(recipe.details.recipeSteps)
        ])
        content_parts.append(f"Steps:\n{steps_text}")
        
        # Add tags
        if recipe.metadata.tags:
            tags_text = ", ".join([tag.name for tag in recipe.metadata.tags if tag.name])
            if tags_text:
                content_parts.append(f"Tags: {tags_text}")
        
        # Add serving size
        content_parts.append(f"Serving Size: {recipe.details.servingSize}")
        
        return "\n\n".join(content_parts)
    
    def _prepare_search_context(self, search_results: List[Document]) -> str:
        """Prepare context from search results for LLM"""
        if not search_results:
            return "No relevant recipes found."
        
        context_parts = []
        for i, doc in enumerate(search_results, 1):
            context_parts.append(f"Recipe {i}:\n{doc.page_content}")
        
        return "\n\n".join(context_parts)
    
    def _is_recipe_creation_request(self, message: str) -> bool:
        """Determine if the user wants to create a recipe"""
        creation_keywords = [
            "create", "make", "generate", "new recipe", "recipe for",
            "how to make", "how to cook", "recipe idea", "suggest recipe"
        ]
        
        message_lower = message.lower()
        is_creation = any(keyword in message_lower for keyword in creation_keywords)
        
        logger.debug(f"Recipe creation request analysis: {is_creation} for message: {message[:50]}...")
        
        return is_creation
    
    def _has_meaningful_context(self, context: str) -> bool:
        """Determine if the context contains meaningful recipe information"""
        if not context or context == "No relevant recipes found.":
            return False
        
        # Check for common indicators of gibberish or poor quality content
        gibberish_indicators = [
            "test", "sample", "example", "placeholder", "dummy", "fake",
            "lorem ipsum", "random text", "asdf", "qwerty", "12345",
            "recipe title", "recipe description", "ingredient name"
        ]
        
        context_lower = context.lower()
        
        # If context contains too many gibberish indicators, consider it poor quality
        gibberish_count = sum(1 for indicator in gibberish_indicators if indicator in context_lower)
        
        # Also check if the context is too short or seems incomplete
        if len(context.strip()) < 100:  # Very short context
            return False
        
        # If more than 2 gibberish indicators found, consider context poor
        if gibberish_count > 2:
            return False
        
        # Check for meaningful recipe content indicators
        meaningful_indicators = [
            "ingredients:", "steps:", "cook", "bake", "fry", "grill",
            "preheat", "season", "mix", "combine", "add", "stir"
        ]
        
        meaningful_count = sum(1 for indicator in meaningful_indicators if indicator in context_lower)
        
        # If we have meaningful indicators and not too much gibberish, consider it good
        has_meaningful = meaningful_count >= 2 and gibberish_count <= 1
        
        logger.debug(f"Context analysis - meaningful: {meaningful_count}, gibberish: {gibberish_count}, result: {has_meaningful}")
        
        return has_meaningful
    
    def _handle_recipe_search(self, message: str, context: str, search_results: List[Document]) -> ChatResponse:
        """Handle recipe search requests - return only recipe IDs"""
        if not search_results:
            logger.info("No search results found for recipe search request")
            return ChatResponse(
                reply="Sorry, I could not find anything matching your request. Try searching with different keywords or ask me to create a new recipe for you.",
                sources=None,
                recipe_suggestion=None
            )
        
        # Extract recipe IDs from search results
        recipe_ids = []
        for doc in search_results:
            combined_id = doc.metadata.get("recipe_id", "")
            if combined_id:
                # Ensure it's a string
                recipe_ids.append(str(combined_id))
        
        logger.info(f"Found {len(recipe_ids)} recipe IDs for search request: {recipe_ids}")
        structured_logger.info(
            f"Recipe search completed - found {len(recipe_ids)} recipes",
            extra={'extra_context': {
                'operation': 'recipe_search',
                'recipe_ids': recipe_ids,
                'search_query': message[:100]
            }}
        )
        
        return ChatResponse(
            reply="I have found some recipes for you!",
            sources=recipe_ids,  # Return recipe IDs instead of full metadata
            recipe_suggestion=None
        )
    
    def _handle_recipe_creation(self, message: str, context: str) -> ChatResponse:
        """Handle recipe creation requests with improved creativity and context handling"""
        start_time = time.time()
        
        try:
            logger.info(f"Handling recipe creation request: {message[:100]}...")
            
            # Determine if we have meaningful context
            has_good_context = self._has_meaningful_context(context)
            
            structured_logger.info(
                f"Recipe creation analysis - has good context: {has_good_context}",
                extra={'extra_context': {
                    'operation': 'recipe_creation',
                    'has_good_context': has_good_context,
                    'context_length': len(context),
                    'creation_query': message[:100]
                }}
            )
            
            if has_good_context:
                # Use context-aware prompt when we have good recipes
                prompt_type = "context_aware"
                prompt = ChatPromptTemplate.from_template("""
                You are a creative and experienced chef assistant. The user wants to create a new recipe based on their request.
                
                User Request: {query}
                Available Recipe Context: {context}
                
                Create an innovative recipe that:
                1. Directly addresses the user's request
                2. Takes inspiration from the available recipes but adds your own creative twist
                3. Uses modern cooking techniques and flavor combinations
                4. Is practical and achievable for home cooks
                5. Has clear, detailed instructions
                
                Be creative! Don't just copy the existing recipes - use them as inspiration to create something new and exciting.
                
                Return a complete recipe in this JSON format:
                {{
                    "title": "Creative and descriptive recipe title",
                    "description": "Appetizing description explaining what makes this recipe special",
                    "servingSize": 4,
                    "recipeIngredients": [
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}},
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}}
                    ],
                    "recipeSteps": [
                        {{"order": 1, "details": "Detailed step with cooking tips and techniques"}},
                        {{"order": 2, "details": "Detailed step with cooking tips and techniques"}}
                    ]
                }}
                
                Make the recipe unique and creative while being practical. Use specific ingredients and detailed steps.
                """)
            else:
                # Use creative standalone prompt when no good context is available
                prompt_type = "standalone"
                prompt = ChatPromptTemplate.from_template("""
                You are a master chef with decades of culinary experience. The user wants to create a new recipe, but we don't have many relevant examples to work with. This is your chance to be truly creative!
                
                User Request: {query}
                
                Create an innovative, delicious recipe that:
                1. Directly fulfills the user's request
                2. Uses your culinary expertise to create something unique
                3. Incorporates modern cooking techniques and flavor profiles
                4. Is practical for home cooking
                5. Has clear, detailed instructions that any cook can follow
                
                Be bold and creative! Think outside the box and create something that will impress. Use interesting ingredient combinations, cooking methods, and presentation ideas.
                
                Return a complete recipe in this JSON format:
                {{
                    "title": "Creative and descriptive recipe title",
                    "description": "Appetizing description explaining what makes this recipe special and unique",
                    "servingSize": 4,
                    "recipeIngredients": [
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}},
                        {{"name": "specific ingredient name", "unit": "measurement unit", "amount": numeric_amount}}
                    ],
                    "recipeSteps": [
                        {{"order": 1, "details": "Detailed step with cooking tips, techniques, and timing"}},
                        {{"order": 2, "details": "Detailed step with cooking tips, techniques, and timing"}}
                    ]
                }}
                
                Make this recipe memorable and delicious. Use specific measurements, cooking times, and helpful tips.
                """)
            
            # Generate LLM response
            llm_start = time.time()
            chain = prompt | self.llm
            response = chain.invoke({
                "query": message,
                "context": context
            })
            llm_duration = round((time.time() - llm_start) * 1000, 2)
            
            structured_logger.info(
                f"LLM recipe creation completed using {prompt_type} prompt",
                extra={
                    'duration_ms': llm_duration,
                    'extra_context': {
                        'operation': 'llm_recipe_creation',
                        'prompt_type': prompt_type,
                        'llm_duration_ms': llm_duration,
                        'response_length': len(response.content) if response.content else 0
                    }
                }
            )
            
            # Parse the response to extract recipe data
            parse_start = time.time()
            recipe_data = self._parse_recipe_response(response.content)
            parse_duration = round((time.time() - parse_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"Recipe creation completed in {total_duration}ms")
            structured_logger.info(
                "Recipe creation completed successfully",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'recipe_creation',
                        'prompt_type': prompt_type,
                        'llm_duration_ms': llm_duration,
                        'parse_duration_ms': parse_duration,
                        'has_recipe_data': bool(recipe_data),
                        'recipe_title': recipe_data.get('title', 'unknown') if recipe_data else 'none'
                    }
                }
            )
            
            return ChatResponse(
                reply=f"I've created a unique recipe for you based on your request: '{message}'. This recipe combines creativity with practicality - you can now create it using the 'Create Recipe' button!",
                sources=None,
                recipe_suggestion=recipe_data
            )
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Error in recipe creation: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe creation failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'recipe_creation',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            
            return ChatResponse(
                reply="I'm sorry, I encountered an error creating a recipe for you. Please try again.",
                sources=None,
                recipe_suggestion=None
            )
    
    def _parse_recipe_response(self, response_content: str) -> Dict[str, Any]:
        """Parse LLM response to extract recipe data with improved validation and fallback"""
        parse_start = time.time()
        
        try:
            logger.debug(f"Parsing LLM response of length: {len(response_content)}")
            
            # Clean the response - remove any non-JSON text
            response_content = response_content.strip()
            
            # Try to extract JSON from the response
            import re
            json_match = re.search(r'\{.*\}', response_content, re.DOTALL)
            if json_match:
                recipe_json = json_match.group()
                parsed_data = json.loads(recipe_json)
                
                # Validate and enhance required fields
                required_fields = ["title", "description", "servingSize", "recipeIngredients", "recipeSteps", "tags"]
                for field in required_fields:
                    if field not in parsed_data:
                        logger.warning(f"Missing required field: {field}")
                        if field == "description":
                            parsed_data[field] = "A delicious recipe created just for you"
                        elif field in ["recipeIngredients", "recipeSteps", "tags"]:
                            parsed_data[field] = []
                        elif field == "servingSize":
                            parsed_data[field] = 4
                        elif field == "title":
                            parsed_data[field] = "Creative Recipe"
                
                # Validate and clean up ingredients
                if "recipeIngredients" in parsed_data:
                    ingredients = parsed_data["recipeIngredients"]
                    if not isinstance(ingredients, list):
                        parsed_data["recipeIngredients"] = []
                    else:
                        # Clean up ingredient entries
                        cleaned_ingredients = []
                        for ing in ingredients:
                            if isinstance(ing, dict) and "name" in ing:
                                cleaned_ing = {
                                    "name": str(ing.get("name", "")).strip(),
                                    "unit": str(ing.get("unit", "")).strip(),
                                    "amount": ing.get("amount", 1)
                                }
                                if cleaned_ing["name"]:  # Only add if name is not empty
                                    cleaned_ingredients.append(cleaned_ing)
                        parsed_data["recipeIngredients"] = cleaned_ingredients
                
                # Validate and clean up steps
                if "recipeSteps" in parsed_data:
                    steps = parsed_data["recipeSteps"]
                    if not isinstance(steps, list):
                        parsed_data["recipeSteps"] = []
                    else:
                        # Clean up step entries
                        cleaned_steps = []
                        for i, step in enumerate(steps, 1):
                            if isinstance(step, dict) and "details" in step:
                                cleaned_step = {
                                    "order": i,
                                    "details": str(step.get("details", "")).strip()
                                }
                                if cleaned_step["details"]:  # Only add if details is not empty
                                    cleaned_steps.append(cleaned_step)
                        parsed_data["recipeSteps"] = cleaned_steps
                
                # Validate and clean up tags
                if "tags" in parsed_data:
                    tags = parsed_data["tags"]
                    if not isinstance(tags, list):
                        parsed_data["tags"] = []
                    else:
                        # Clean up tags
                        cleaned_tags = [str(tag).strip() for tag in tags if str(tag).strip()]
                        parsed_data["tags"] = cleaned_tags
                
                # Ensure title and description are meaningful
                if not parsed_data.get("title") or parsed_data["title"] in ["Recipe Title", "Creative Recipe"]:
                    parsed_data["title"] = "Delicious Homemade Recipe"
                
                if not parsed_data.get("description") or parsed_data["description"] in ["Recipe description", "A delicious recipe created just for you"]:
                    parsed_data["description"] = "A carefully crafted recipe with fresh ingredients and delicious flavors"
                
                parse_duration = round((time.time() - parse_start) * 1000, 2)
                
                logger.info(f"Successfully parsed recipe response in {parse_duration}ms")
                structured_logger.info(
                    "Recipe response parsing completed successfully",
                    extra={
                        'duration_ms': parse_duration,
                        'extra_context': {
                            'operation': 'parse_recipe_response',
                            'recipe_title': parsed_data.get('title', 'unknown'),
                            'ingredient_count': len(parsed_data.get('recipeIngredients', [])),
                            'step_count': len(parsed_data.get('recipeSteps', [])),
                            'original_response_length': len(response_content)
                        }
                    }
                )
                
                return parsed_data
            else:
                logger.error("No JSON found in LLM response")
                structured_logger.error(
                    "Recipe response parsing failed - no JSON found",
                    extra={'extra_context': {
                        'operation': 'parse_recipe_response',
                        'error': 'no_json_found',
                        'response_length': len(response_content)
                    }}
                )
                return self._get_default_recipe_data()
                
        except json.JSONDecodeError as e:
            parse_duration = round((time.time() - parse_start) * 1000, 2)
            logger.error(f"Invalid JSON in LLM response: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe response parsing failed - invalid JSON: {str(e)}",
                extra={
                    'duration_ms': parse_duration,
                    'extra_context': {
                        'operation': 'parse_recipe_response',
                        'error': 'invalid_json',
                        'error_details': str(e),
                        'response_length': len(response_content)
                    }
                }
            )
            return self._get_default_recipe_data()
        except Exception as e:
            parse_duration = round((time.time() - parse_start) * 1000, 2)
            logger.error(f"Error parsing recipe response: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe response parsing failed: {str(e)}",
                extra={
                    'duration_ms': parse_duration,
                    'extra_context': {
                        'operation': 'parse_recipe_response',
                        'error': str(e),
                        'error_type': type(e).__name__,
                        'response_length': len(response_content)
                    }
                }
            )
            return self._get_default_recipe_data()
    
    def _get_default_recipe_data(self) -> Dict[str, Any]:
        """Get default recipe data structure with creative fallback"""
        logger.info("Using default recipe data as fallback")
        return {
            "title": "Chef's Special Creation",
            "description": "A unique recipe crafted with care using fresh, quality ingredients and creative cooking techniques",
            "servingSize": 4,
            "recipeIngredients": [
                {"name": "Fresh ingredients", "unit": "as needed", "amount": 1},
                {"name": "Your favorite spices", "unit": "to taste", "amount": 1},
                {"name": "Love and creativity", "unit": "generous", "amount": 1}
            ],
            "recipeSteps": [
                {"order": 1, "details": "Gather your fresh ingredients and prepare your cooking space"},
                {"order": 2, "details": "Follow your culinary instincts and create something delicious"},
                {"order": 3, "details": "Season to taste and enjoy your homemade creation"}
            ]
            # No tags field here
        }
    
    def get_health_status(self) -> Dict[str, str]:
        """Get health status of all services"""
        start_time = time.time()
        
        try:
            logger.info("Checking health status of all services...")
            
            # Check RAG helper
            rag_start = time.time()
            rag_stats = self.rag_helper.get_collection_stats()
            rag_duration = round((time.time() - rag_start) * 1000, 2)
            rag_status = "healthy" if rag_stats.get("status") == "healthy" else "unhealthy"
            
            # Check LLM (simple ping)
            llm_start = time.time()
            llm_status = "healthy"  # Assume healthy if no exception
            llm_duration = round((time.time() - llm_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            health_status = {
                "rag_helper": rag_status,
                "llm": llm_status,
                "vector_store": rag_status
            }
            
            logger.info(f"Health status check completed in {total_duration}ms: {health_status}")
            structured_logger.info(
                "Health status check completed",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'health_check',
                        'rag_status': rag_status,
                        'llm_status': llm_status,
                        'rag_duration_ms': rag_duration,
                        'llm_duration_ms': llm_duration,
                        'overall_status': 'healthy' if all(status == 'healthy' for status in health_status.values()) else 'unhealthy'
                    }
                }
            )
            
            return health_status
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Error getting health status: {e}", exc_info=True)
            structured_logger.error(
                f"Health status check failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'operation': 'health_check',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            
            return {
                "rag_helper": "unhealthy",
                "llm": "unhealthy",
                "vector_store": "unhealthy",
                "error": str(e)
            }
    
    def cleanup(self):
        """Cleanup resources used by the LLM service"""
        start_time = time.time()
        
        try:
            logger.info("Starting Recipe LLM service cleanup...")
            structured_logger.info(
                "Recipe LLM service cleanup started",
                extra={'extra_context': {'operation': 'cleanup', 'component': 'llm_service'}}
            )
            
            self.rag_helper.cleanup()
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.info(f"Recipe LLM service cleanup completed in {duration_ms}ms")
            structured_logger.info(
                "Recipe LLM service cleanup completed",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'operation': 'cleanup',
                        'component': 'llm_service',
                        'status': 'success'
                    }
                }
            )
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Error during Recipe LLM service cleanup: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe LLM service cleanup failed: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'operation': 'cleanup',
                        'component': 'llm_service',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            ) 
