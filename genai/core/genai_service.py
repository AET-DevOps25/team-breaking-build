import uuid
from typing import List, Optional, Dict, Any
from datetime import datetime

# LangChain imports
from langchain_ollama.chat_models import ChatOllama
from langchain_ollama import OllamaEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Weaviate
from langchain.schema import Document
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate
from langchain.memory import ConversationBufferMemory
from langchain.schema.messages import HumanMessage, AIMessage

import weaviate
from loguru import logger

from config import config
from models.schemas import (
    RecipeData,
    ChatMessage,
    ChatRequest,
    ChatResponse,
    RecipeCreationRequest,
    RecipeCreationResponse,
    RecipeMetadataDTO,
    RecipeDetailsDTO,
    RecipeIngredientDTO,
    RecipeStepDTO,
    RecipeTagDTO
)
from services.recipe_integration import RecipeServiceIntegration, parse_llm_recipe_response

class GenAIService:
    """Core GenAI service using LLM for chatbot and recipe creation"""
    
    def __init__(self):
        self.llm = None
        self.embeddings = None
        self.vector_store = None
        self.retriever = None
        self.conversations = {}
        self.recipe_service = RecipeServiceIntegration()
        self.initialize_services()
    
    def initialize_services(self):
        """Initialize LLM and vector store services"""
        try:
            logger.info("Initializing LLM")
            self.llm = ChatOllama(
                model=config.LLM_CHAT_MODEL,
                base_url=config.LLM_BASE_URL,
                temperature=0.7
            )
            
            # Initialize embeddings
            logger.info("Initializing embeddings with LLM")
            self.embeddings = OllamaEmbeddings(
                model=config.LLM_EMBEDDING_MODEL,
                base_url=config.LLM_BASE_URL
            )
            
            # Initialize Weaviate vector store using current API
            logger.info("Initializing Weaviate vector store")
            
            try:
                # Create Weaviate client
                if config.WEAVIATE_API_KEY:
                    client = weaviate.connect_to_wcs(
                        cluster_url=config.WEAVIATE_URL,
                        auth_credentials=weaviate.auth.Auth.api_key(config.WEAVIATE_API_KEY),
                        headers={
                            "X-OpenAI-Api-Key": config.LLM_API_KEY
                        } if config.LLM_API_KEY else {}
                    )
                else:
                    # For local development without API key
                    client = weaviate.connect_to_local(
                        host=config.WEAVIATE_URL.replace("http://", "").replace("https://", ""),
                        headers={
                            "X-OpenAI-Api-Key": config.LLM_API_KEY
                        } if config.LLM_API_KEY else {}
                    )
                
                # Create or get collection
                collection_name = config.WEAVIATE_INDEX_NAME
                if not client.collections.exists(collection_name):
                    logger.info(f"Creating collection: {collection_name}")
                    client.collections.create(
                        name=collection_name,
                        properties=[
                            {"name": "title", "dataType": ["text"]},
                            {"name": "description", "dataType": ["text"]},
                            {"name": "ingredients", "dataType": ["text[]"]},
                            {"name": "steps", "dataType": ["text[]"]},
                            {"name": "tags", "dataType": ["text[]"]},
                            {"name": "recipe_id", "dataType": ["int"]},
                            {"name": "user_id", "dataType": ["int"]},
                            {"name": "serving_size", "dataType": ["int"]},
                        ],
                        vectorizer_config=weaviate.config.Configure.Vectorizer.text2vec_openai(),
                        module_config=weaviate.config.Configure.Module(
                            name="text2vec-openai",
                            tag="latest",
                            repo="weaviate/text2vec-openai"
                        )
                    )
                
                # Initialize LangChain Weaviate integration
                self.vector_store = Weaviate(
                    client=client,
                    index_name=collection_name,
                    text_key="content",
                    embedding=self.embeddings
                )
                
                self.retriever = self.vector_store.as_retriever(
                    search_type="similarity",
                    search_kwargs={"k": 5}
                )
                
                logger.info("Weaviate vector store initialized successfully")
                
            except Exception as weaviate_error:
                logger.warning(f"Failed to initialize Weaviate: {weaviate_error}")
                logger.info("Falling back to in-memory vector store for development")
                # Fallback to simple in-memory storage for development
                self.vector_store = None
                self.retriever = None
            
            logger.info("GenAI service initialized successfully")
            
        except Exception as e:
            logger.error(f"Error initializing GenAI service: {e}")
            raise
    
    async def chat(self, request: ChatRequest) -> ChatResponse:
        """Handle chat conversation with recipe search and creation capabilities"""
        try:
            conversation_id = request.conversation_id or str(uuid.uuid4())
            
            # Get or create conversation memory
            if conversation_id not in self.conversations:
                self.conversations[conversation_id] = ConversationBufferMemory(
                    memory_key="chat_history",
                    return_messages=True
                )
            
            memory = self.conversations[conversation_id]
            
            # Check if user wants to create a recipe
            if self._is_recipe_creation_request(request.message):
                return await self._handle_recipe_creation(request, conversation_id)
            
            # Search for relevant recipes
            relevant_recipes = await self._search_recipes(request.message)
            
            # Create context from relevant recipes
            context = self._create_context_from_recipes(relevant_recipes)
            
            # Create chat prompt
            chat_prompt = PromptTemplate(
                input_variables=["chat_history", "user_message", "recipe_context"],
                template="""
                You are a helpful cooking assistant. You can help users find recipes and create new ones.
                
                Previous conversation:
                {chat_history}
                
                Available recipes context:
                {recipe_context}
                
                User: {user_message}
                Assistant: """
            )
            
            # Generate response
            chain = LLMChain(llm=self.llm, prompt=chat_prompt, memory=memory)
            response = await chain.arun(
                user_message=request.message,
                recipe_context=context
            )
            
            # Update conversation memory
            memory.chat_memory.add_user_message(request.message)
            memory.chat_memory.add_ai_message(response)
            
            return ChatResponse(
                reply=response,
                conversation_id=conversation_id,
                sources=[{"title": recipe.title, "id": recipe.id} for recipe in relevant_recipes[:3]]
            )
            
        except Exception as e:
            logger.error(f"Error in chat: {e}")
            return ChatResponse(
                reply="I'm sorry, I encountered an error. Please try again.",
                conversation_id=request.conversation_id or str(uuid.uuid4())
            )
    
    async def create_recipe(self, request: RecipeCreationRequest) -> RecipeCreationResponse:
        """Create a new recipe using the LLM"""
        try:
            prompt_text = f"""Create a complete recipe based on the following requirements:

Description: {request.description}
Ingredients: {', '.join(request.ingredients)}
Dietary Restrictions: {', '.join(request.dietary_restrictions) if request.dietary_restrictions else 'None'}
Cuisine Type: {request.cuisine_type or 'General'}
Difficulty: {request.difficulty or 'Medium'}
Serving Size: {request.serving_size or 4}

Please format the recipe as follows:

Title: [Recipe Name]

Description: [Brief description of the recipe]

Serving Size: [Number of servings]

Ingredients:
- [Amount] [Unit] [Ingredient Name]
- [Amount] [Unit] [Ingredient Name]
...

Instructions:
- [Step 1 details]
- [Step 2 details]
- [Step 3 details]
...

Make sure the recipe is complete, practical, and follows standard cooking practices."""

            # Generate recipe using LLM
            response = await self.llm.agenerate([[HumanMessage(content=prompt_text)]])
            recipe_text = response.generations[0][0].text
            
            # Parse the LLM response into proper DTOs
            metadata, details = parse_llm_recipe_response(recipe_text)
            metadata.userId = request.user_id
            
            # Add tags based on cuisine type and dietary restrictions
            tags = []
            if request.cuisine_type:
                tags.append(RecipeTagDTO(name=request.cuisine_type.lower()))
            if request.dietary_restrictions:
                for restriction in request.dietary_restrictions:
                    tags.append(RecipeTagDTO(name=restriction.lower()))
            metadata.tags = tags
            
            # Create recipe in the recipe service
            created_recipe = await self.recipe_service.create_recipe(metadata, details)
            
            if created_recipe:
                return RecipeCreationResponse(
                    recipe={
                        "id": created_recipe.id,
                        "title": created_recipe.title,
                        "description": created_recipe.description,
                        "ingredients": details.recipeIngredients,
                        "steps": details.recipeSteps,
                        "tags": [tag.name for tag in created_recipe.tags],
                        "serving_size": details.servingSize
                    }
                )
            else:
                raise Exception("Failed to create recipe in recipe service")
                
        except Exception as e:
            logger.error(f"Error creating recipe: {e}")
            raise
    
    async def index_recipe(self, recipe_data: RecipeData) -> bool:
        """Index a recipe in the vector store"""
        try:
            if not self.vector_store:
                logger.warning("Vector store not available, skipping indexing")
                return False
                
            # Create document for indexing
            ingredients_text = ", ".join([
                f"{ing.name} {ing.amount or ''} {ing.unit or ''}".strip()
                for ing in recipe_data.ingredients
            ])
            
            steps_text = " ".join([
                f"{step.order}. {step.details}"
                for step in recipe_data.steps
            ])
            
            tags_text = ", ".join(recipe_data.tags)
            
            document_text = f"""
            Title: {recipe_data.title}
            Description: {recipe_data.description or ''}
            Ingredients: {ingredients_text}
            Steps: {steps_text}
            Tags: {tags_text}
            Serving Size: {recipe_data.serving_size or 'Not specified'}
            """
            
            # Create document
            document = Document(
                page_content=document_text,
                metadata={
                    "id": recipe_data.id,
                    "title": recipe_data.title,
                    "user_id": recipe_data.user_id,
                    "tags": recipe_data.tags,
                    "serving_size": recipe_data.serving_size
                }
            )
            
            # Add to vector store
            self.vector_store.add_documents([document])
            logger.info(f"Indexed recipe {recipe_data.id}: {recipe_data.title}")
            return True
            
        except Exception as e:
            logger.error(f"Error indexing recipe {recipe_data.id}: {e}")
            return False
    
    async def sync_recipes_from_service(self) -> int:
        """Sync all recipes from the recipe service to the vector store"""
        try:
            recipes = await self.recipe_service.sync_all_recipes()
            indexed_count = 0
            
            for recipe in recipes:
                # Convert to RecipeData format
                recipe_data = RecipeData(
                    id=recipe.id,
                    title=recipe.title,
                    description=recipe.description,
                    ingredients=[],  # We don't have details in metadata
                    steps=[],        # We don't have details in metadata
                    tags=[tag.name for tag in recipe.tags],
                    serving_size=recipe.servingSize,
                    user_id=recipe.userId
                )
                
                if await self.index_recipe(recipe_data):
                    indexed_count += 1
            
            logger.info(f"Synced {indexed_count} recipes to vector store")
            return indexed_count
            
        except Exception as e:
            logger.error(f"Error syncing recipes: {e}")
            return 0
    
    def _is_recipe_creation_request(self, message: str) -> bool:
        """Check if the user message is requesting recipe creation"""
        creation_keywords = [
            "create", "make", "new", "generate", "recipe", "cook", "prepare", "dish"
        ]
        
        message_lower = message.lower()
        # Check if message contains at least 2 creation keywords
        keyword_count = sum(1 for keyword in creation_keywords if keyword in message_lower)
        return keyword_count >= 2
    
    async def _handle_recipe_creation(self, request: ChatRequest, conversation_id: str) -> ChatResponse:
        """Handle recipe creation request in chat"""
        try:
            # Extract recipe requirements from the message
            recipe_request = RecipeCreationRequest(
                description=request.message,
                ingredients=[],  # Would need more sophisticated parsing
                user_id=int(request.user_id) if request.user_id.isdigit() else 1
            )
            
            # Create the recipe
            recipe_response = await self.create_recipe(recipe_request)
            
            # Create a friendly response
            response_text = f"""I've created a recipe for you! Here's what I made:

**{recipe_response.recipe['title']}**

{recipe_response.recipe['description']}

**Ingredients:**
{chr(10).join([f"• {ing['name']} {ing['amount'] or ''} {ing['unit'] or ''}".strip() for ing in recipe_response.recipe['ingredients']])}

**Instructions:**
{chr(10).join([f"{i+1}. {step['details']}" for i, step in enumerate(recipe_response.recipe['steps'])])}

The recipe has been saved to your collection!"""
            
            return ChatResponse(
                reply=response_text,
                conversation_id=conversation_id,
                recipe_created=recipe_response.recipe
            )
            
        except Exception as e:
            logger.error(f"Error handling recipe creation: {e}")
            return ChatResponse(
                reply="I'm sorry, I couldn't create the recipe. Please try again with more specific details.",
                conversation_id=conversation_id
            )
    
    async def _search_recipes(self, query: str) -> List[RecipeMetadataDTO]:
        """Search for relevant recipes"""
        try:
            if not self.retriever:
                logger.warning("Vector store not available, returning empty results")
                return []
                
            # Use vector store to find relevant recipes
            docs = self.retriever.get_relevant_documents(query)
            
            # Extract recipe IDs from documents
            recipe_ids = []
            for doc in docs:
                recipe_id = doc.metadata.get("id")
                if recipe_id:
                    recipe_ids.append(recipe_id)
            
            # Fetch full recipe data from recipe service
            recipes = []
            for recipe_id in recipe_ids[:5]:  # Limit to top 5
                recipe = await self.recipe_service.get_recipe(recipe_id)
                if recipe:
                    recipes.append(recipe)
            
            return recipes
            
        except Exception as e:
            logger.error(f"Error searching recipes: {e}")
            return []
    
    def _create_context_from_recipes(self, recipes: List[RecipeMetadataDTO]) -> str:
        """Create context string from relevant recipes"""
        if not recipes:
            return "No relevant recipes found."
        
        context_parts = []
        for recipe in recipes:
            tags_text = ", ".join([tag.name for tag in recipe.tags])
            context_parts.append(f"Recipe: {recipe.title}\nDescription: {recipe.description or 'No description'}\nTags: {tags_text}")
        
        return "\n\n".join(context_parts)
    
    async def close(self):
        """Clean up resources"""
        try:
            await self.recipe_service.close()
        except Exception as e:
            logger.error(f"Error closing GenAI service: {e}") 