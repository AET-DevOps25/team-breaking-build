import os
import logging
from typing import List, Dict, Any
import weaviate
from weaviate.classes.query import Filter
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_weaviate.vectorstores import WeaviateVectorStore
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document
from dotenv import load_dotenv

# Setup shared embeddings model
load_dotenv()
embeddings_model = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")

# Disable Huggingface's tokenizer parallelism (avoid deadlocks caused by process forking in langchain)
os.environ["TOKENIZERS_PARALLELISM"] = "false"

logger = logging.getLogger(__name__)

class RAGHelper:
    """
    A helper for the retrieval stage of the RAG pipeline for recipe search and generation.
    """
    
    def __init__(self, weaviate_host: str = None, weaviate_port: int = None):
        """Initialize RAG helper with Weaviate client and embeddings"""
        try:
            # Get weaviate configuration from environment or use defaults
            self.weaviate_host = weaviate_host or os.getenv("WEAVIATE_HOST", "weaviate")
            self.weaviate_port = weaviate_port or int(os.getenv("WEAVIATE_PORT", "8080"))
            
            # Initialize Weaviate client
            self._initialize_weaviate_client()
            
            # Initialize vector store with proper schema
            self._setup_vector_store()
            
            logger.info("RAG helper initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize RAG helper: {e}")
            raise
    
    def _initialize_weaviate_client(self):
        """Initialize Weaviate client connection"""
        try:
            # Connect to weaviate using configured host and port
            self.weaviate_client = weaviate.connect_to_local(
                host=self.weaviate_host,
                port=self.weaviate_port
            )
            logger.info(f"Connected to Weaviate service: {self.weaviate_host}:{self.weaviate_port}")
            
        except Exception as e:
            logger.error(f"Failed to connect to Weaviate: {e}")
            raise
    
    def _setup_vector_store(self):
        """Setup Weaviate vector store for recipes with proper schema"""
        try:
            # Check if collection already exists (case insensitive)
            collections = self.weaviate_client.collections.list_all()
            logger.info(f"Existing collections: {collections}")
            
            # Check for any variation of "recipes" collection name
            collection_exists = any(
                col.lower() in ["recipes", "recipe"] for col in collections
            )
            
            if collection_exists:
                # Collection exists, just get it
                self.db = WeaviateVectorStore(
                    client=self.weaviate_client,
                    index_name="recipes",
                    embedding=embeddings_model,
                    text_key="text"
                )
                logger.info("Using existing recipes collection")
            else:
                # Create new collection with proper schema for string recipe_id
                self._create_collection_with_schema()
                logger.info("Created new recipes collection with string recipe_id schema")
            
        except Exception as e:
            logger.error(f"Failed to setup vector store: {e}")
            raise
    
    def _create_collection_with_schema(self):
        """Create Weaviate collection with proper schema for string recipe_id"""
        try:
            # Create the collection with proper property definitions
            self.weaviate_client.collections.create(
                name="recipes",
                properties=[
                    weaviate.classes.config.Property(
                        name="recipe_id",
                        data_type=weaviate.classes.config.DataType.TEXT,
                        description="Combined recipe and branch ID"
                    ),
                    weaviate.classes.config.Property(
                        name="title",
                        data_type=weaviate.classes.config.DataType.TEXT,
                        description="Recipe title"
                    ),
                    weaviate.classes.config.Property(
                        name="description",
                        data_type=weaviate.classes.config.DataType.TEXT,
                        description="Recipe description"
                    ),
                    weaviate.classes.config.Property(
                        name="ingredients",
                        data_type=weaviate.classes.config.DataType.TEXT_ARRAY,
                        description="List of ingredients"
                    ),
                    weaviate.classes.config.Property(
                        name="steps",
                        data_type=weaviate.classes.config.DataType.TEXT_ARRAY,
                        description="List of cooking steps"
                    ),
                    weaviate.classes.config.Property(
                        name="tags",
                        data_type=weaviate.classes.config.DataType.TEXT_ARRAY,
                        description="Recipe tags"
                    ),
                    weaviate.classes.config.Property(
                        name="user_id",
                        data_type=weaviate.classes.config.DataType.INT,
                        description="User ID who created the recipe"
                    ),
                    weaviate.classes.config.Property(
                        name="serving_size",
                        data_type=weaviate.classes.config.DataType.INT,
                        description="Number of servings"
                    ),
                    weaviate.classes.config.Property(
                        name="is_placeholder",
                        data_type=weaviate.classes.config.DataType.BOOL,
                        description="Flag for placeholder documents"
                    )
                ],
                vectorizer_config=weaviate.classes.config.Configure.Vectorizer.TEXT2VEC_HUGGINGFACE
            )
            
            # Initialize vector store
            self.db = WeaviateVectorStore(
                client=self.weaviate_client,
                index_name="recipes",
                embedding=embeddings_model,
                text_key="text"
            )
            
            logger.info("Created recipes collection with string recipe_id schema")
            
        except Exception as e:
            logger.error(f"Failed to create collection with schema: {e}")
            raise
    
    def add_recipe(self, recipe_content: str, metadata: Dict[str, Any]) -> bool:
        """
        Add a recipe to the vector store.
        
        Args:
            recipe_content: The recipe content to vectorize.
            metadata: Metadata for the recipe (includes combined recipe_id).
        
        Returns:
            True if successful, False otherwise.
        """
        try:
            # Create document
            doc = Document(
                page_content=recipe_content,
                metadata=metadata
            )
            
            # Add to vector store
            self.db.add_documents([doc])
            
            logger.info(f"Added recipe {metadata.get('recipe_id')} to vector store")
            return True
            
        except Exception as e:
            logger.error(f"Failed to add recipe to vector store: {e}")
            return False
    
    def retrieve(self, query: str, top_k: int = 5) -> List[Document]:
        """
        Retrieve relevant documents from the vector store based on a query.
        
        Args:
            query: The search query.
            top_k: The number of top results to return.
        
        Returns:
            List of retrieved documents.
        """
        try:
            results = self.db.similarity_search(query, k=top_k)
            logger.info(f"Retrieved {len(results)} documents for query: {query}")
            return results
            
        except Exception as e:
            logger.error(f"Failed to retrieve documents: {e}")
            return []
    
    def delete_recipe(self, combined_id: str) -> bool:
        """
        Delete a recipe from the vector store by combined ID.
        
        Args:
            combined_id: The combined recipe+branch ID (format: "recipeID+branchID").
        
        Returns:
            True if successful, False otherwise.
        """
        try:
            # Delete by exact combined recipe_id
            self.weaviate_client.collections.get("recipes").data.delete_many(
                where=Filter.by_property("recipe_id").equal(combined_id)
            )
            
            logger.info(f"Deleted recipe {combined_id} from vector store")
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete recipe {combined_id}: {e}")
            return False
    
    def delete_recipe_by_recipe_id(self, recipe_id: str) -> bool:
        """
        Delete all recipes from the vector store by recipe ID (matches any branch).
        
        Args:
            recipe_id: The recipe ID to match against.
        
        Returns:
            True if successful, False otherwise.
        """
        try:
            # Delete by recipe_id pattern (matches "recipeID+*" where recipeID matches exactly)
            # Use like operator to match the part before "+" equals the recipe_id
            pattern = f"{recipe_id}+*"
            self.weaviate_client.collections.get("recipes").data.delete_many(
                where=Filter.by_property("recipe_id").like(pattern)
            )
            
            logger.info(f"Deleted all recipes with recipe_id {recipe_id} from vector store")
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete recipes with recipe_id {recipe_id}: {e}")
            return False
    
    def get_collection_stats(self) -> Dict[str, Any]:
        """
        Get vector store statistics.
        
        Returns:
            Dictionary with collection statistics.
        """
        try:
            collection = self.weaviate_client.collections.get("recipes")
            stats = collection.aggregate.over_all()
            
            return {
                "collection_name": "recipes",
                "total_objects": stats.total,
                "status": "healthy"
            }
            
        except Exception as e:
            logger.error(f"Failed to get collection stats: {e}")
            return {
                "collection_name": "recipes",
                "total_objects": 0,
                "status": "unhealthy",
                "error": str(e)
            }
    
    def cleanup(self):
        """
        Clean up the Weaviate client connection.
        """
        try:
            self.weaviate_client.close()
            logger.info("Weaviate client connection closed.")
        except Exception as e:
            logger.error(f"Error closing Weaviate client: {e}") 
