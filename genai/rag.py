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
    
    def __init__(self):
        """Initialize RAG helper with Weaviate client and embeddings"""
        try:
            # Initialize Weaviate client
            self._initialize_weaviate_client()
            
            # Initialize vector store
            self._setup_vector_store()
            
            logger.info("RAG helper initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize RAG helper: {e}")
            raise
    
    def _initialize_weaviate_client(self):
        """Initialize Weaviate client connection"""
        try:
            # Check if we're running in Docker (service name available)
            if os.path.exists('/.dockerenv'):
                # Running in Docker - connect to service name
                self.weaviate_client = weaviate.connect_to_local(
                    host="weaviate",
                    port=8082
                )
                logger.info("Connected to Weaviate service in Docker: weaviate:8082")
            else:
                # Running locally - connect to localhost
                self.weaviate_client = weaviate.connect_to_local(port=8082)
                logger.info("Connected to local Weaviate on port 8082")
            
        except Exception as e:
            logger.error(f"Failed to connect to Weaviate: {e}")
            raise
    
    def _setup_vector_store(self):
        """Setup Weaviate vector store for recipes"""
        try:
            # Create vector store with a sample recipe document to establish schema
            sample_recipe_doc = Document(
                page_content="Sample Recipe: Placeholder recipe for schema initialization",
                metadata={
                    "recipe_id": 0,
                    "title": "Schema Initialization Recipe",
                    "description": "Placeholder",
                    "ingredients": ["placeholder"],
                    "steps": ["placeholder step"],
                    "tags": ["initialization"],
                    "user_id": 0,
                    "serving_size": 1,
                    "is_placeholder": True
                }
            )
            
            # Initialize vector store using from_documents
            self.db = WeaviateVectorStore.from_documents(
                documents=[sample_recipe_doc],
                embedding=embeddings_model,
                client=self.weaviate_client,
                index_name="recipes"
            )
            
            # Remove the placeholder document
            try:
                self.weaviate_client.collections.get("recipes").data.delete_many(
                    where=Filter.by_property("is_placeholder").equal(True)
                )
            except Exception as e:
                logger.warning(f"Could not remove placeholder document: {e}")
            
            logger.info("Created and initialized vector store collection: recipes")
            
        except Exception as e:
            logger.error(f"Failed to setup vector store: {e}")
            raise
    
    def add_recipe(self, recipe_content: str, metadata: Dict[str, Any]) -> bool:
        """
        Add a recipe to the vector store.
        
        Args:
            recipe_content: The recipe content to vectorize.
            metadata: Metadata for the recipe.
        
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
    
    def delete_recipe(self, recipe_id: int) -> bool:
        """
        Delete a recipe from the vector store.
        
        Args:
            recipe_id: The ID of the recipe to delete.
        
        Returns:
            True if successful, False otherwise.
        """
        try:
            # Delete by recipe_id filter
            self.weaviate_client.collections.get("recipes").data.delete_many(
                where=Filter.by_property("recipe_id").equal(recipe_id)
            )
            
            logger.info(f"Deleted recipe {recipe_id} from vector store")
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete recipe {recipe_id}: {e}")
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
