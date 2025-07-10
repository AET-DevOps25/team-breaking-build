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
            # Initialize LLM with environment variables (no defaults)
            self.llm = ChatOpenAI(
                model=os.getenv("LLM_MODEL"),
                temperature=float(os.getenv("LLM_TEMPERATURE")),
                api_key=os.getenv("OPEN_WEBUI_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL")
            )
            
            # Initialize RAG helper with weaviate configuration
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
            
            # Prepare metadata with combined ID format: "recipeID+branchID"
            # Handle both string and integer IDs
            recipe_id = str(recipe.metadata.id) if recipe.metadata.id else "0"
            
            # Use recipe_id as the combined ID (no forking concept in vector store)
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
            success = self.rag_helper.add_recipe(content, metadata)
            
            if success:
                logger.info(f"Indexed recipe {combined_id}: {recipe.metadata.title}")
            else:
                logger.error(f"Failed to index recipe {combined_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"Failed to index recipe {recipe.metadata.id}: {e}")
            return False
    
    def delete_recipe(self, recipe_id: str) -> bool:
        """Delete a recipe from the vector store"""
        try:
            # Delete by recipe_id filter (will match any recipe with this recipe_id in combined ID)
            success = self.rag_helper.delete_recipe_by_recipe_id(recipe_id)
            
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
        """Generate a recipe suggestion based on query and similar recipes with improved creativity"""
        try:
            # Search for similar recipes
            search_results = self.rag_helper.retrieve(query, top_k=3)
            
            # Prepare context from search results
            context = self._prepare_search_context(search_results)
            
            # Determine if we have meaningful context
            has_good_context = self._has_meaningful_context(context)
            
            if has_good_context:
                # Use context-aware prompt when we have good recipes
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
                # Use creative standalone prompt when no good context is available
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
            
            chain = prompt | self.llm
            
            response = chain.invoke({
                "query": query,
                "context": context
            })
            
            # Parse the response to extract recipe data
            recipe_data = self._parse_recipe_response(response.content)
            
            return RecipeSuggestionResponse(
                suggestion=f"I've created a unique recipe suggestion for you based on your request: '{query}'. This recipe combines creativity with practicality!",
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
        return any(keyword in message_lower for keyword in creation_keywords)
    
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
        return meaningful_count >= 2 and gibberish_count <= 1
    
    def _handle_recipe_search(self, message: str, context: str, search_results: List[Document]) -> ChatResponse:
        """Handle recipe search requests - return only recipe IDs"""
        if not search_results:
            return ChatResponse(
                reply="I couldn't find any recipes matching your request. Try searching with different keywords or ask me to create a new recipe for you.",
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
        
        return ChatResponse(
            reply=f"I found {len(recipe_ids)} recipes matching your request. Here are the recipe IDs: {', '.join(recipe_ids)}",
            sources=recipe_ids,  # Return recipe IDs instead of full metadata
            recipe_suggestion=None
        )
    
    def _handle_recipe_creation(self, message: str, context: str) -> ChatResponse:
        """Handle recipe creation requests with improved creativity and context handling"""
        
        # Determine if we have meaningful context
        has_good_context = self._has_meaningful_context(context)
        
        if has_good_context:
            # Use context-aware prompt when we have good recipes
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
        
        chain = prompt | self.llm
        
        response = chain.invoke({
            "query": message,
            "context": context
        })
        
        # Parse the response to extract recipe data
        recipe_data = self._parse_recipe_response(response.content)
        
        return ChatResponse(
            reply=f"I've created a unique recipe for you based on your request: '{message}'. This recipe combines creativity with practicality - you can now create it using the 'Create Recipe' button!",
            sources=None,
            recipe_suggestion=recipe_data
        )
    
    def _parse_recipe_response(self, response_content: str) -> Dict[str, Any]:
        """Parse LLM response to extract recipe data with improved validation and fallback"""
        try:
            import json
            import re
            
            # Clean the response - remove any non-JSON text
            response_content = response_content.strip()
            
            # Try to extract JSON from the response
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
                
                return parsed_data
            else:
                logger.error("No JSON found in LLM response")
                return self._get_default_recipe_data()
                
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in LLM response: {e}")
            return self._get_default_recipe_data()
        except Exception as e:
            logger.error(f"Error parsing recipe response: {e}")
            return self._get_default_recipe_data()
    
    def _get_default_recipe_data(self) -> Dict[str, Any]:
        """Get default recipe data structure with creative fallback"""
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
