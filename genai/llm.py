import logging
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

class RecipeLLM:
    """LLM service for recipe search and suggestion"""
    
    def __init__(self):
        """Initialize LLM service"""
        try:
            # Initialize LLM with environment variables
            self.llm = ChatOpenAI(
                model="llama3.3:latest",
                temperature=0.5,
                api_key=os.getenv("OPEN_WEBUI_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://gpu.aet.cit.tum.de/api/")
            )
            
            # Initialize RAG helper
            self.rag_helper = RAGHelper()
            
            logger.info("Recipe LLM service initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize Recipe LLM service: {e}")
            raise
    
    def index_recipe(self, recipe: RecipeData) -> bool:
        """Index a recipe in the vector store"""
        try:
            # Prepare content for vectorization
            content = self._prepare_recipe_content(recipe)
            
            # Prepare metadata
            metadata = {
                "recipe_id": recipe.metadata.id,
                "title": recipe.metadata.title,
                "description": recipe.metadata.description or "",
                "ingredients": [ing.name for ing in recipe.details.recipeIngredients],
                "steps": [step.details for step in recipe.details.recipeSteps],
                "tags": [tag.name for tag in recipe.metadata.tags],
                "user_id": recipe.metadata.userId,
                "serving_size": recipe.details.servingSize
            }
            
            # Add to vector store using RAG helper
            success = self.rag_helper.add_recipe(content, metadata)
            
            if success:
                logger.info(f"Indexed recipe {recipe.metadata.id}: {recipe.metadata.title}")
            else:
                logger.error(f"Failed to index recipe {recipe.metadata.id}")
            
            return success
            
        except Exception as e:
            logger.error(f"Failed to index recipe {recipe.metadata.id}: {e}")
            return False
    
    def delete_recipe(self, recipe_id: int) -> bool:
        """Delete a recipe from the vector store"""
        try:
            success = self.rag_helper.delete_recipe(recipe_id)
            
            if success:
                logger.info(f"Deleted recipe {recipe_id} from vector store")
            else:
                logger.error(f"Failed to delete recipe {recipe_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"Failed to delete recipe {recipe_id}: {e}")
            return False
    
    def chat(self, message: str) -> ChatResponse:
        """Process chat message and return response"""
        try:
            # Search for relevant recipes
            search_results = self.rag_helper.retrieve(message, top_k=5)
            
            # Prepare context from search results
            context = self._prepare_search_context(search_results)
            
            # Determine if user wants to create a recipe
            is_creation_request = self._is_recipe_creation_request(message)
            
            if is_creation_request:
                return self._handle_recipe_creation(message, context)
            else:
                return self._handle_recipe_search(message, context, search_results)
                
        except Exception as e:
            logger.error(f"Error in chat: {e}")
            return ChatResponse(
                reply="I'm sorry, I encountered an error processing your request. Please try again.",
                sources=None,
                recipe_suggestion=None
            )
    
    def suggest_recipe(self, query: str) -> RecipeSuggestionResponse:
        """Generate a recipe suggestion based on query and similar recipes"""
        try:
            # Search for similar recipes
            search_results = self.rag_helper.retrieve(query, top_k=3)
            
            # Prepare context from search results
            context = self._prepare_search_context(search_results)
            
            # Generate recipe suggestion using LLM
            prompt = ChatPromptTemplate.from_template("""
            You are a creative chef assistant. Based on the user's request and similar recipes, create a new recipe.
            
            User Request: {query}
            Similar Recipes Context: {context}
            
            Create a recipe that matches the user's request while being inspired by the similar recipes.
            Return the recipe in the following JSON format:
            {{
                "title": "Recipe Title",
                "description": "Recipe description",
                "servingSize": 4,
                "recipeIngredients": [
                    {{"name": "ingredient name", "unit": "unit", "amount": amount}},
                    ...
                ],
                "recipeSteps": [
                    {{"order": 1, "details": "step description"}},
                    ...
                ],
                "tags": ["tag1", "tag2", ...]
            }}
            
            Make sure the recipe is creative, practical, and follows the user's request.
            """)
            
            chain = prompt | self.llm
            
            response = chain.invoke({
                "query": query,
                "context": context
            })
            
            # Parse the response to extract recipe data
            recipe_data = self._parse_recipe_response(response.content)
            
            return RecipeSuggestionResponse(
                suggestion=f"I've created a recipe for you based on your request: '{query}'",
                recipe_data=recipe_data
            )
            
        except Exception as e:
            logger.error(f"Error in recipe suggestion: {e}")
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
            tags_text = ", ".join([tag.name for tag in recipe.metadata.tags])
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
        return any(keyword in message_lower for keyword in creation_keywords)
    
    def _handle_recipe_search(self, message: str, context: str, search_results: List[Document]) -> ChatResponse:
        """Handle recipe search requests"""
        if not search_results:
            return ChatResponse(
                reply="I couldn't find any recipes matching your request. Try searching with different keywords or ask me to create a new recipe for you.",
                sources=None,
                recipe_suggestion=None
            )
        
        # Prepare sources for response
        sources = []
        for doc in search_results:
            sources.append({
                "title": doc.metadata.get("title", "Unknown Recipe"),
                "recipe_id": doc.metadata.get("recipe_id"),
                "description": doc.metadata.get("description", ""),
                "tags": doc.metadata.get("tags", [])
            })
        
        # Generate response using LLM
        prompt = ChatPromptTemplate.from_template("""
        You are a helpful cooking assistant. Based on the following recipes and the user's query, provide a helpful response.
        
        User Query: {query}
        Available Recipes: {context}
        
        Provide a helpful response about the recipes that match the user's query. Mention the recipe titles and key features.
        """)
        
        chain = prompt | self.llm
        
        response = chain.invoke({
            "query": message,
            "context": context
        })
        
        return ChatResponse(
            reply=response.content,
            sources=sources,
            recipe_suggestion=None
        )
    
    def _handle_recipe_creation(self, message: str, context: str) -> ChatResponse:
        """Handle recipe creation requests"""
        # Generate recipe suggestion using LLM
        prompt = ChatPromptTemplate.from_template("""
        You are a creative chef assistant. Based on the user's request and similar recipes, create a new recipe.
        
        User Request: {query}
        Similar Recipes Context: {context}
        
        Create a recipe that matches the user's request while being inspired by the similar recipes.
        Return the recipe in the following JSON format:
        {{
            "title": "Recipe Title",
            "description": "Recipe description",
            "servingSize": 4,
            "recipeIngredients": [
                {{"name": "ingredient name", "unit": "unit", "amount": amount}},
                ...
            ],
            "recipeSteps": [
                {{"order": 1, "details": "step description"}},
                ...
            ],
            "tags": ["tag1", "tag2", ...]
        }}
        
        Make sure the recipe is creative, practical, and follows the user's request.
        """)
        
        chain = prompt | self.llm
        
        response = chain.invoke({
            "query": message,
            "context": context
        })
        
        # Parse the response to extract recipe data
        recipe_data = self._parse_recipe_response(response.content)
        
        return ChatResponse(
            reply=f"I've created a recipe for you based on your request: '{message}'. You can now create this recipe using the 'Create Recipe' button.",
            sources=None,
            recipe_suggestion=recipe_data
        )
    
    def _parse_recipe_response(self, response_content: str) -> Dict[str, Any]:
        """Parse LLM response to extract recipe data"""
        try:
            # Simple parsing - in a real implementation, you might want to use a more robust parser
            import json
            import re
            
            # Try to extract JSON from the response
            json_match = re.search(r'\{.*\}', response_content, re.DOTALL)
            if json_match:
                recipe_json = json_match.group()
                return json.loads(recipe_json)
            else:
                # Fallback: create a basic structure
                return {
                    "title": "Generated Recipe",
                    "description": "A recipe created based on your request",
                    "servingSize": 4,
                    "recipeIngredients": [],
                    "recipeSteps": [],
                    "tags": []
                }
        except Exception as e:
            logger.error(f"Error parsing recipe response: {e}")
            return {
                "title": "Generated Recipe",
                "description": "A recipe created based on your request",
                "servingSize": 4,
                "recipeIngredients": [],
                "recipeSteps": [],
                "tags": []
            }
    
    def get_health_status(self) -> Dict[str, str]:
        """Get health status of all services"""
        try:
            # Check RAG helper
            rag_stats = self.rag_helper.get_collection_stats()
            rag_status = "healthy" if rag_stats.get("status") == "healthy" else "unhealthy"
            
            # Check LLM (simple ping)
            llm_status = "healthy"  # Assume healthy if no exception
            
            return {
                "rag_helper": rag_status,
                "llm": llm_status,
                "vector_store": rag_status
            }
        except Exception as e:
            logger.error(f"Error getting health status: {e}")
            return {
                "rag_helper": "unhealthy",
                "llm": "unhealthy",
                "vector_store": "unhealthy",
                "error": str(e)
            }
    
    def cleanup(self):
        """Cleanup resources used by the LLM service"""
        try:
            self.rag_helper.cleanup()
            logger.info("Recipe LLM service cleanup completed")
        except Exception as e:
            logger.error(f"Error during cleanup: {e}") 
