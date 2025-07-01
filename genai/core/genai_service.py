import logging
from typing import List, Dict
import weaviate
from weaviate.classes.init import Auth
from langchain_community.vectorstores import Weaviate
from langchain_ollama import OllamaEmbeddings, ChatOllama
from langchain.schema import Document
from langchain.prompts import ChatPromptTemplate
from langchain.schema.output_parser import StrOutputParser

from config import settings
from models.schemas import RecipeData, ChatResponse

logger = logging.getLogger(__name__)

class GenAIService:
    """Simplified GenAI service for recipe vectorization and LLM interactions"""
    
    def __init__(self):
        self.weaviate_client = None
        self.vector_store = None
        self.embeddings = None
        self.llm = None
        self._initialize_services()
    
    def _initialize_services(self):
        """Initialize Weaviate client, vector store, and LLM"""
        try:
            # Initialize Weaviate client
            auth_config = Auth.api_key(settings.weaviate_api_key) if settings.weaviate_api_key else None
            self.weaviate_client = weaviate.connect_to_wcs(
                cluster_url=settings.weaviate_url,
                auth_credentials=auth_config
            ) if settings.weaviate_api_key else weaviate.connect_to_local(
                host=settings.weaviate_url.replace("http://", "").replace("https://", "")
            )
            
            # Initialize embeddings
            self.embeddings = OllamaEmbeddings(
                model=settings.llm_model,
                base_url=settings.llm_base_url
            )
            
            # Initialize LLM
            self.llm = ChatOllama(
                model=settings.llm_model,
                base_url=settings.llm_base_url,
                temperature=settings.llm_temperature
            )
            
            # Create or get vector store
            self._setup_vector_store()
            
            logger.info("GenAI service initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize GenAI service: {e}")
            raise
    
    def _setup_vector_store(self):
        """Setup Weaviate vector store with collection"""
        try:
            # Check if collection exists, create if not
            collections = self.weaviate_client.collections.list_all()
            collection_exists = any(col.name == settings.weaviate_collection_name for col in collections)
            
            if not collection_exists:
                # Create collection with schema
                self.weaviate_client.collections.create(
                    name=settings.weaviate_collection_name,
                    properties=[
                        {"name": "recipe_id", "dataType": ["int"]},
                        {"name": "title", "dataType": ["text"]},
                        {"name": "description", "dataType": ["text"]},
                        {"name": "ingredients", "dataType": ["text[]"]},
                        {"name": "steps", "dataType": ["text[]"]},
                        {"name": "tags", "dataType": ["text[]"]},
                        {"name": "user_id", "dataType": ["int"]},
                        {"name": "serving_size", "dataType": ["int"]},
                        {"name": "content", "dataType": ["text"]}
                    ],
                    vectorizer_config=weaviate.classes.config.Configure.Vectorizer.text2vec_transformers()
                )
                logger.info(f"Created Weaviate collection: {settings.weaviate_collection_name}")
            
            # Initialize vector store
            self.vector_store = Weaviate(
                client=self.weaviate_client,
                index_name=settings.weaviate_collection_name,
                text_key="content",
                embedding=self.embeddings
            )
            
        except Exception as e:
            logger.error(f"Failed to setup vector store: {e}")
            raise
    
    def index_recipe(self, recipe: RecipeData) -> bool:
        """Index a recipe in the vector store"""
        try:
            # Prepare content for vectorization
            content = self._prepare_recipe_content(recipe)
            
            # Create document for vector store
            doc = Document(
                page_content=content,
                metadata={
                    "recipe_id": recipe.metadata.id,
                    "title": recipe.metadata.title,
                    "description": recipe.metadata.description or "",
                    "ingredients": [ing.name for ing in recipe.details.recipeIngredients],
                    "steps": [step.details for step in recipe.details.recipeSteps],
                    "tags": [tag.name for tag in recipe.metadata.tags],
                    "user_id": recipe.metadata.userId,
                    "serving_size": recipe.details.servingSize
                }
            )
            
            # Add to vector store
            self.vector_store.add_documents([doc])
            
            logger.info(f"Indexed recipe {recipe.metadata.id}: {recipe.metadata.title}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to index recipe {recipe.metadata.id}: {e}")
            return False
    
    def delete_recipe(self, recipe_id: int) -> bool:
        """Delete a recipe from the vector store"""
        try:
            # Delete by metadata filter
            self.weaviate_client.collections.get(settings.weaviate_collection_name).data.delete_many(
                where=weaviate.classes.query.Filter.by_property("recipe_id").equal(recipe_id)
            )
            
            logger.info(f"Deleted recipe {recipe_id} from vector store")
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete recipe {recipe_id}: {e}")
            return False
    
    def chat(self, message: str, user_id: str) -> ChatResponse:
        """Process chat message and return response"""
        try:
            # Search for relevant recipes
            search_results = self.vector_store.similarity_search(
                message,
                k=settings.max_search_results
            )
            
            # Prepare context from search results
            context = self._prepare_search_context(search_results)
            
            # Determine if user wants to create a recipe
            is_creation_request = self._is_recipe_creation_request(message)
            
            if is_creation_request:
                return self._handle_recipe_creation(message, context, user_id)
            else:
                return self._handle_recipe_search(message, context, search_results)
                
        except Exception as e:
            logger.error(f"Error in chat: {e}")
            return ChatResponse(
                reply="I'm sorry, I encountered an error processing your request. Please try again.",
                sources=None,
                recipe_suggestion=None
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
        steps_text = " ".join([f"{i+1}. {step.details}" for i, step in enumerate(recipe.details.recipeSteps)])
        content_parts.append(f"Steps: {steps_text}")
        
        # Add tags
        if recipe.metadata.tags:
            tags_text = ", ".join([tag.name for tag in recipe.metadata.tags])
            content_parts.append(f"Tags: {tags_text}")
        
        return " | ".join(content_parts)
    
    def _prepare_search_context(self, search_results: List[Document]) -> str:
        """Prepare context from search results"""
        if not search_results:
            return "No recipes found."
        
        context_parts = []
        for i, doc in enumerate(search_results, 1):
            metadata = doc.metadata
            context_parts.append(
                f"Recipe {i}: {metadata.get('title', 'Unknown')} - "
                f"Ingredients: {', '.join(metadata.get('ingredients', []))} - "
                f"Tags: {', '.join(metadata.get('tags', []))}"
            )
        
        return "\n".join(context_parts)
    
    def _is_recipe_creation_request(self, message: str) -> bool:
        """Check if user wants to create a recipe"""
        creation_keywords = [
            "create", "make", "new recipe", "recipe for", "how to make",
            "cook", "prepare", "dish", "meal", "food"
        ]
        message_lower = message.lower()
        return any(keyword in message_lower for keyword in creation_keywords)
    
    def _handle_recipe_search(self, message: str, context: str, search_results: List[Document]) -> ChatResponse:
        """Handle recipe search requests"""
        prompt = ChatPromptTemplate.from_template("""
        You are a helpful recipe assistant. Based on the user's query and the available recipes, provide a helpful response.
        
        User query: {message}
        
        Available recipes:
        {context}
        
        Provide a helpful response about the recipes that match the user's query. If no recipes match, suggest alternatives or ask for clarification.
        """)
        
        chain = prompt | self.llm | StrOutputParser()
        reply = chain.invoke({"message": message, "context": context})
        
        # Prepare sources with recipe IDs
        sources = []
        for doc in search_results:
            sources.append({
                "title": doc.metadata.get("title", "Unknown"),
                "recipe_id": doc.metadata.get("recipe_id"),
                "similarity_score": 0.8  # Placeholder
            })
        
        return ChatResponse(
            reply=reply,
            sources=sources if sources else None,
            recipe_suggestion=None
        )
    
    def _handle_recipe_creation(self, message: str, context: str, user_id: str) -> ChatResponse:
        """Handle recipe creation requests"""
        prompt = ChatPromptTemplate.from_template("""
        You are a recipe creation assistant. Based on the user's request and similar recipes, create a new recipe in JSON format.
        
        User request: {message}
        
        Similar recipes for reference:
        {context}
        
        IMPORTANT: You must return ONLY valid JSON with the exact structure shown below. Do not include any additional text, explanations, or markdown formatting.
        
        STRICT JSON STRUCTURE:
        {{
            "metadata": {{
                "title": "Recipe Title",
                "description": "Recipe description",
                "userId": {user_id},
                "tags": [{{"name": "tag1"}}, {{"name": "tag2"}}]
            }},
            "details": {{
                "servingSize": 4,
                "recipeIngredients": [
                    {{"name": "ingredient name", "unit": "unit", "amount": 1.0}}
                ],
                "recipeSteps": [
                    {{"order": 1, "details": "Step description"}}
                ]
            }}
        }}
        
        CRITICAL RULES:
        1. Return ONLY the JSON, no other text
        2. Use exact field names as shown above
        3. Ensure all JSON syntax is valid
        4. Include realistic ingredients and steps
        5. Make sure all required fields are present
        """)
        
        chain = prompt | self.llm | StrOutputParser()
        recipe_json = chain.invoke({"message": message, "context": context, "user_id": user_id})
        
        return ChatResponse(
            reply="I have come up with a recipe for you. Would you like to look at it?",
            sources=None,
            recipe_suggestion={"recipe_data": recipe_json}
        )
    
    def get_health_status(self) -> Dict[str, str]:
        """Get health status of all services"""
        status = {}
        
        # Check Weaviate
        try:
            self.weaviate_client.collections.list_all()
            status["weaviate"] = "healthy"
        except Exception as e:
            status["weaviate"] = f"unhealthy: {str(e)}"
        
        # Check LLM
        try:
            # Simple test call
            test_prompt = ChatPromptTemplate.from_template("Say 'OK'")
            chain = test_prompt | self.llm | StrOutputParser()
            chain.invoke({})
            status["llm"] = "healthy"
        except Exception as e:
            status["llm"] = f"unhealthy: {str(e)}"
        
        return status 