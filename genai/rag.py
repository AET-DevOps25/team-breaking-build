import os
import logging
import time
import json
from typing import List, Dict, Any
import weaviate
import weaviate.classes.config as wc
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
# Create structured logger for detailed logging
structured_logger = logging.getLogger("structured")

class RAGHelper:
    """
    A helper for the retrieval stage of the RAG pipeline for recipe search and generation.
    """
    
    def __init__(self, weaviate_host: str = None, weaviate_port: int = None, weaviate_grpc_port: int = None):
        """Initialize RAG helper with Weaviate client and embeddings"""
        start_time = time.time()
        
        try:
            logger.info("Starting RAG helper initialization...")
            structured_logger.info(
                "RAG helper initialization started",
                extra={'extra_context': {'component': 'rag_helper', 'phase': 'initialization'}}
            )
            
            # Get weaviate configuration from environment or use defaults
            self.weaviate_host = weaviate_host or os.getenv("WEAVIATE_HOST", "weaviate")
            self.weaviate_port = weaviate_port or int(os.getenv("WEAVIATE_PORT", "8080"))
            self.weaviate_grpc_port = weaviate_grpc_port or int(os.getenv("WEAVIATE_GRPC_PORT", "50051"))
            
            structured_logger.info(
                f"RAG helper configuration: host={self.weaviate_host}, port={self.weaviate_port}, grpc_port={self.weaviate_grpc_port}",
                extra={'extra_context': {
                    'component': 'rag_helper',
                    'weaviate_host': self.weaviate_host,
                    'weaviate_port': self.weaviate_port,
                    'weaviate_grpc_port': self.weaviate_grpc_port
                }}
            )
            
            # Initialize Weaviate client
            self._initialize_weaviate_client()
            
            # Initialize vector store with proper schema
            self._setup_vector_store()
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.info(f"RAG helper initialized successfully in {duration_ms}ms")
            structured_logger.info(
                "RAG helper initialization completed",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'rag_helper',
                        'phase': 'initialization',
                        'status': 'success'
                    }
                }
            )
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to initialize RAG helper: {e}", exc_info=True)
            structured_logger.error(
                f"RAG helper initialization failed: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'rag_helper',
                        'phase': 'initialization',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            raise
    
    def _initialize_weaviate_client(self):
        """Initialize Weaviate client connection"""
        start_time = time.time()
        
        try:
            logger.info(f"Connecting to Weaviate at {self.weaviate_host}:{self.weaviate_port}...")
            structured_logger.info(
                f"Weaviate connection attempt started",
                extra={'extra_context': {
                    'component': 'weaviate_client',
                    'operation': 'connect',
                    'host': self.weaviate_host,
                    'port': self.weaviate_port,
                    'grpc_port': self.weaviate_grpc_port
                }}
            )
            
            # Connect to weaviate using configured host and port
            self.weaviate_client = weaviate.connect_to_local(
                host=self.weaviate_host,
                port=self.weaviate_port,
                grpc_port=self.weaviate_grpc_port
            )
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.info(f"Connected to Weaviate service: {self.weaviate_host}:{self.weaviate_port} in {duration_ms}ms")
            structured_logger.info(
                "Weaviate connection established successfully",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'weaviate_client',
                        'operation': 'connect',
                        'status': 'success',
                        'host': self.weaviate_host,
                        'port': self.weaviate_port
                    }
                }
            )
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to connect to Weaviate: {e}", exc_info=True)
            structured_logger.error(
                f"Weaviate connection failed: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'weaviate_client',
                        'operation': 'connect',
                        'status': 'failed',
                        'host': self.weaviate_host,
                        'port': self.weaviate_port,
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            raise
    
    def _setup_vector_store(self):
        """Setup Weaviate vector store for recipes with proper schema"""
        start_time = time.time()
        
        try:
            logger.info("Setting up vector store...")
            structured_logger.info(
                "Vector store setup started",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'setup'
                }}
            )
            
            # Check if collection already exists (case insensitive)
            collections_start = time.time()
            collections = self.weaviate_client.collections.list_all()
            collections_duration = round((time.time() - collections_start) * 1000, 2)
            
            logger.info(f"Found existing collections: {collections} (checked in {collections_duration}ms)")
            structured_logger.info(
                f"Collection discovery completed",
                extra={
                    'duration_ms': collections_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'list_collections',
                        'collections_found': collections,
                        'collection_count': len(collections)
                    }
                }
            )
            
            # Check for any variation of "recipes" collection name
            collection_exists = any(
                col.lower() in ["recipes", "recipe"] for col in collections
            )
            
            if collection_exists:
                logger.info("Using existing recipes collection")
                structured_logger.info(
                    "Using existing recipes collection",
                    extra={'extra_context': {
                        'component': 'vector_store',
                        'operation': 'use_existing_collection',
                        'collection_name': 'recipes'
                    }}
                )
                
                # Collection exists, just get it
                self.db = WeaviateVectorStore(
                    client=self.weaviate_client,
                    index_name="recipes",
                    embedding=embeddings_model,
                    text_key="text"
                )
            else:
                logger.info("Creating new recipes collection with schema...")
                structured_logger.info(
                    "Creating new recipes collection",
                    extra={'extra_context': {
                        'component': 'vector_store',
                        'operation': 'create_collection',
                        'collection_name': 'recipes'
                    }}
                )
                
                # Create new collection with proper schema for string recipe_id
                self._create_collection_with_schema()
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.info(f"Vector store setup completed in {duration_ms}ms")
            structured_logger.info(
                "Vector store setup completed",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'setup',
                        'status': 'success',
                        'collection_existed': collection_exists
                    }
                }
            )
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to setup vector store: {e}", exc_info=True)
            structured_logger.error(
                f"Vector store setup failed: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'setup',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            raise
    
    def _create_collection_with_schema(self):
        """Create Weaviate collection with proper schema for string recipe_id"""
        start_time = time.time()
        
        try:
            logger.info("Creating recipes collection with schema...")
            structured_logger.info(
                "Collection creation started",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'create_collection_schema',
                    'collection_name': 'recipes'
                }}
            )
            
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
                vectorizer_config=wc.Configure.Vectorizer.text2vec_transformers()
            )
            
            # Initialize vector store
            self.db = WeaviateVectorStore(
                client=self.weaviate_client,
                index_name="recipes",
                embedding=embeddings_model,
                text_key="text"
            )
            
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.info(f"Created recipes collection with string recipe_id schema in {duration_ms}ms")
            structured_logger.info(
                "Collection creation completed successfully",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'create_collection_schema',
                        'collection_name': 'recipes',
                        'status': 'success',
                        'properties_count': 8
                    }
                }
            )
            
        except Exception as e:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to create collection with schema: {e}", exc_info=True)
            structured_logger.error(
                f"Collection creation failed: {str(e)}",
                extra={
                    'duration_ms': duration_ms,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'create_collection_schema',
                        'collection_name': 'recipes',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
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
        start_time = time.time()
        recipe_id = metadata.get('recipe_id', 'unknown')
        
        try:
            logger.info(f"Adding recipe to vector store: {recipe_id}")
            structured_logger.info(
                f"Recipe addition started: {recipe_id}",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'add_recipe',
                    'recipe_id': recipe_id,
                    'recipe_title': metadata.get('title', 'unknown'),
                    'content_length': len(recipe_content),
                    'metadata_keys': list(metadata.keys())
                }}
            )
            
            # Create document
            doc_creation_start = time.time()
            doc = Document(
                page_content=recipe_content,
                metadata=metadata
            )
            doc_creation_duration = round((time.time() - doc_creation_start) * 1000, 2)
            
            # Add to vector store
            vector_store_start = time.time()
            self.db.add_documents([doc])
            vector_store_duration = round((time.time() - vector_store_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"Successfully added recipe {recipe_id} to vector store in {total_duration}ms")
            structured_logger.info(
                f"Recipe addition completed successfully: {recipe_id}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'add_recipe',
                        'recipe_id': recipe_id,
                        'recipe_title': metadata.get('title', 'unknown'),
                        'status': 'success',
                        'content_length': len(recipe_content),
                        'doc_creation_duration_ms': doc_creation_duration,
                        'vector_store_duration_ms': vector_store_duration,
                        'ingredient_count': len(metadata.get('ingredients', [])),
                        'step_count': len(metadata.get('steps', []))
                    }
                }
            )
            
            return True
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to add recipe {recipe_id} to vector store: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe addition failed: {recipe_id} - {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'add_recipe',
                        'recipe_id': recipe_id,
                        'recipe_title': metadata.get('title', 'unknown'),
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__,
                        'content_length': len(recipe_content)
                    }
                }
            )
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
        start_time = time.time()
        
        try:
            logger.info(f"Retrieving {top_k} documents for query: {query[:100]}...")
            structured_logger.info(
                f"Document retrieval started: {query[:100]}...",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'retrieve',
                    'query_length': len(query),
                    'query_preview': query[:100],
                    'top_k': top_k
                }}
            )
            
            # Perform similarity search
            similarity_start = time.time()
            results = self.db.similarity_search(query, k=top_k)
            similarity_duration = round((time.time() - similarity_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            # Extract result metadata for logging
            result_metadata = []
            for doc in results:
                result_metadata.append({
                    'recipe_id': doc.metadata.get('recipe_id', 'unknown'),
                    'title': doc.metadata.get('title', 'unknown'),
                    'content_length': len(doc.page_content)
                })
            
            logger.info(f"Retrieved {len(results)} documents for query in {total_duration}ms")
            structured_logger.info(
                f"Document retrieval completed: found {len(results)} documents",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'retrieve',
                        'query_length': len(query),
                        'query_preview': query[:100],
                        'top_k': top_k,
                        'results_count': len(results),
                        'similarity_duration_ms': similarity_duration,
                        'result_metadata': result_metadata[:3]  # Log first 3 results
                    }
                }
            )
            
            return results
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to retrieve documents: {e}", exc_info=True)
            structured_logger.error(
                f"Document retrieval failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'retrieve',
                        'query_length': len(query),
                        'query_preview': query[:100],
                        'top_k': top_k,
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            return []
    
    def delete_recipe(self, combined_id: str) -> bool:
        """
        Delete a recipe from the vector store by combined ID.
        
        Args:
            combined_id: The combined recipe+branch ID (format: "recipeID+branchID").
        
        Returns:
            True if successful, False otherwise.
        """
        start_time = time.time()
        
        try:
            logger.info(f"Deleting recipe by combined ID: {combined_id}")
            structured_logger.info(
                f"Recipe deletion by combined ID started: {combined_id}",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'delete_recipe',
                    'combined_id': combined_id,
                    'deletion_type': 'combined_id'
                }}
            )
            
            # Delete by exact combined recipe_id
            deletion_start = time.time()
            self.weaviate_client.collections.get("recipes").data.delete_many(
                where=Filter.by_property("recipe_id").equal(combined_id)
            )
            deletion_duration = round((time.time() - deletion_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"Successfully deleted recipe {combined_id} from vector store in {total_duration}ms")
            structured_logger.info(
                f"Recipe deletion completed successfully: {combined_id}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'delete_recipe',
                        'combined_id': combined_id,
                        'deletion_type': 'combined_id',
                        'status': 'success',
                        'deletion_duration_ms': deletion_duration
                    }
                }
            )
            
            return True
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to delete recipe {combined_id}: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe deletion failed: {combined_id} - {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'delete_recipe',
                        'combined_id': combined_id,
                        'deletion_type': 'combined_id',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            return False
    
    def delete_recipe_by_recipe_id(self, recipe_id: str) -> bool:
        """
        Delete a recipe from the vector store by recipe ID.
        
        Args:
            recipe_id: The recipe ID to delete.
        
        Returns:
            True if successful, False otherwise.
        """
        start_time = time.time()
        
        try:
            logger.info(f"Deleting recipe by recipe ID: {recipe_id}")
            structured_logger.info(
                f"Recipe deletion by recipe ID started: {recipe_id}",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'delete_recipe_by_recipe_id',
                    'recipe_id': recipe_id,
                    'deletion_type': 'recipe_id'
                }}
            )
            
            # Delete by exact recipe_id match
            deletion_start = time.time()
            self.weaviate_client.collections.get("recipes").data.delete_many(
                where=Filter.by_property("recipe_id").equal(recipe_id)
            )
            deletion_duration = round((time.time() - deletion_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"Successfully deleted recipe with recipe_id {recipe_id} from vector store in {total_duration}ms")
            structured_logger.info(
                f"Recipe deletion completed successfully: {recipe_id}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'delete_recipe_by_recipe_id',
                        'recipe_id': recipe_id,
                        'deletion_type': 'recipe_id',
                        'status': 'success',
                        'deletion_duration_ms': deletion_duration
                    }
                }
            )
            
            return True
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to delete recipe with recipe_id {recipe_id}: {e}", exc_info=True)
            structured_logger.error(
                f"Recipe deletion by recipe ID failed: {recipe_id} - {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'delete_recipe_by_recipe_id',
                        'recipe_id': recipe_id,
                        'deletion_type': 'recipe_id',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            return False
    
    def get_collection_stats(self) -> Dict[str, Any]:
        """
        Get vector store statistics.
        
        Returns:
            Dictionary with collection statistics.
        """
        start_time = time.time()
        
        try:
            logger.info("Retrieving collection statistics...")
            structured_logger.info(
                "Collection statistics retrieval started",
                extra={'extra_context': {
                    'component': 'vector_store',
                    'operation': 'get_collection_stats',
                    'collection_name': 'recipes'
                }}
            )
            
            # Get collection statistics
            stats_start = time.time()
            collection = self.weaviate_client.collections.get("recipes")
            stats = collection.aggregate.over_all()
            stats_duration = round((time.time() - stats_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            result = {
                "collection_name": "recipes",
                "total_objects": stats.total,
                "status": "healthy"
            }
            
            logger.info(f"Retrieved collection statistics in {total_duration}ms: {stats.total} objects")
            structured_logger.info(
                f"Collection statistics retrieval completed",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'get_collection_stats',
                        'collection_name': 'recipes',
                        'status': 'success',
                        'total_objects': stats.total,
                        'stats_duration_ms': stats_duration
                    }
                }
            )
            
            return result
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Failed to get collection stats: {e}", exc_info=True)
            structured_logger.error(
                f"Collection statistics retrieval failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'vector_store',
                        'operation': 'get_collection_stats',
                        'collection_name': 'recipes',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            )
            
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
        start_time = time.time()
        
        try:
            logger.info("Starting RAG helper cleanup...")
            structured_logger.info(
                "RAG helper cleanup started",
                extra={'extra_context': {
                    'component': 'rag_helper',
                    'operation': 'cleanup'
                }}
            )
            
            # Close Weaviate client connection
            cleanup_start = time.time()
            self.weaviate_client.close()
            cleanup_duration = round((time.time() - cleanup_start) * 1000, 2)
            
            total_duration = round((time.time() - start_time) * 1000, 2)
            
            logger.info(f"RAG helper cleanup completed in {total_duration}ms")
            structured_logger.info(
                "RAG helper cleanup completed successfully",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'rag_helper',
                        'operation': 'cleanup',
                        'status': 'success',
                        'cleanup_duration_ms': cleanup_duration
                    }
                }
            )
            
        except Exception as e:
            total_duration = round((time.time() - start_time) * 1000, 2)
            logger.error(f"Error during RAG helper cleanup: {e}", exc_info=True)
            structured_logger.error(
                f"RAG helper cleanup failed: {str(e)}",
                extra={
                    'duration_ms': total_duration,
                    'extra_context': {
                        'component': 'rag_helper',
                        'operation': 'cleanup',
                        'status': 'failed',
                        'error': str(e),
                        'error_type': type(e).__name__
                    }
                }
            ) 
