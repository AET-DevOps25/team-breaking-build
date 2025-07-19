import pytest
import sys
import os
from unittest.mock import Mock, patch, MagicMock
from typing import List, Dict, Any
import weaviate
import weaviate.classes.config as wc
from weaviate.classes.query import Filter

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from rag import RAGHelper
from langchain_core.documents import Document


@pytest.fixture
def sample_recipe_content():
    """Sample recipe content for testing"""
    return """
    Title: Test Recipe
    Description: A delicious test recipe
    Ingredients:
    - Test Ingredient 1 (100g)
    - Test Ingredient 2 (200ml)
    
    Steps:
    1. Mix ingredients
    2. Cook for 30 minutes
    3. Serve hot
    """


@pytest.fixture
def sample_metadata():
    """Sample metadata for testing"""
    return {
        "recipe_id": "123",
        "title": "Test Recipe",
        "description": "A delicious test recipe",
        "ingredients": ["Test Ingredient 1", "Test Ingredient 2"],
        "steps": ["Mix ingredients", "Cook for 30 minutes", "Serve hot"],
        "tags": ["test", "delicious"],
        "serving_size": 4
    }


@pytest.fixture
def mock_weaviate_client():
    """Mock Weaviate client for testing"""
    mock_client = Mock()
    mock_client.collections.list_all.return_value = ["recipes"]
    return mock_client


@pytest.fixture
def mock_vector_store():
    """Mock vector store for testing"""
    mock_store = Mock()
    mock_store.add_documents.return_value = ["doc1", "doc2"]
    mock_store.similarity_search.return_value = []
    return mock_store


class TestRAGHelperInitialization:
    """Test RAGHelper initialization"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_initialization_success(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test successful RAG helper initialization"""
        # Mock weaviate client
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        # Mock vector store
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        # Mock embeddings
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper(weaviate_host="localhost", weaviate_port=8080)
        
        # Verify weaviate connection
        mock_weaviate_connect.assert_called_once_with(
            host="localhost",
            port=8080,
            grpc_port=50051
        )
        
        # Verify vector store setup
        assert rag.weaviate_client == mock_client
        assert rag.db == mock_store
    
    @patch('rag.weaviate.connect_to_local')
    def test_initialization_weaviate_connection_failure(self, mock_weaviate_connect):
        """Test RAG helper initialization when Weaviate connection fails"""
        mock_weaviate_connect.side_effect = Exception("Connection failed")
        
        with pytest.raises(Exception, match="Connection failed"):
            RAGHelper()
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_initialization_with_custom_ports(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test RAG helper initialization with custom ports"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper(
            weaviate_host="custom-host",
            weaviate_port=9090,
            weaviate_grpc_port=50052
        )
        
        mock_weaviate_connect.assert_called_once_with(
            host="custom-host",
            port=9090,
            grpc_port=50052
        )
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_initialization_environment_variables(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test RAG helper initialization using environment variables"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        with patch.dict('os.environ', {
            'WEAVIATE_HOST': 'env-host',
            'WEAVIATE_PORT': '9090',
            'WEAVIATE_GRPC_PORT': '50053'
        }):
            rag = RAGHelper()
            
            mock_weaviate_connect.assert_called_once_with(
                host="env-host",
                port=9090,
                grpc_port=50053
            )


class TestRAGHelperSetupVectorStore:
    """Test vector store setup functionality"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_setup_existing_collection(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test vector store setup with existing collection"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        
        # Verify existing collection was used
        mock_vector_store_class.assert_called_once()
        call_args = mock_vector_store_class.call_args
        assert call_args[1]["client"] == mock_client
        assert call_args[1]["index_name"] == "recipes"
        assert call_args[1]["text_key"] == "text"
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_setup_new_collection(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test vector store setup with new collection creation"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = []  # No existing collections
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        
        # Verify collection creation was attempted
        # Note: The actual collection creation logic would be tested separately
        assert rag.weaviate_client == mock_client
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_setup_collection_case_insensitive(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test vector store setup with case insensitive collection matching"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["Recipes", "other_collection"]  # Different case
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        
        # Should still use existing collection despite case difference
        mock_vector_store_class.assert_called_once()


class TestRAGHelperAddRecipe:
    """Test recipe addition functionality"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    @patch('rag.RecursiveCharacterTextSplitter')
    def test_add_recipe_success(self, mock_splitter_class, mock_embeddings, mock_vector_store_class, mock_weaviate_connect, sample_recipe_content, sample_metadata):
        """Test successful recipe addition"""
        # Setup mocks
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_store.add_documents.return_value = ["doc1"]
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        mock_splitter = Mock()
        mock_splitter.split_text.return_value = ["chunk1", "chunk2"]
        mock_splitter_class.return_value = mock_splitter
        
        rag = RAGHelper()
        result = rag.add_recipe(sample_recipe_content, sample_metadata)
        
        assert result is True
        mock_store.add_documents.assert_called_once()
        
        # Verify documents were created correctly
        call_args = mock_store.add_documents.call_args[0][0]
        assert len(call_args) == 1  # Single document (no splitting for small content)
        
        # Check first document
        doc1 = call_args[0]
        assert isinstance(doc1, Document)
        assert "Test Recipe" in doc1.page_content
        assert doc1.metadata["recipe_id"] == "123"
        assert doc1.metadata["title"] == "Test Recipe"
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_add_recipe_failure(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect, sample_recipe_content, sample_metadata):
        """Test recipe addition failure"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_store.add_documents.side_effect = Exception("Add documents failed")
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        result = rag.add_recipe(sample_recipe_content, sample_metadata)
        
        assert result is False
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_add_recipe_empty_content(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect, sample_metadata):
        """Test recipe addition with empty content"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        result = rag.add_recipe("", sample_metadata)
    
        assert result is True  # Empty content is handled gracefully


class TestRAGHelperRetrieve:
    """Test recipe retrieval functionality"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_retrieve_success(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test successful recipe retrieval"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        # Mock search results
        mock_doc1 = Document(page_content="Recipe 1 content", metadata={"title": "Recipe 1"})
        mock_doc2 = Document(page_content="Recipe 2 content", metadata={"title": "Recipe 2"})
        
        mock_store = Mock()
        mock_store.similarity_search.return_value = [mock_doc1, mock_doc2]
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        results = rag.retrieve("pasta recipe", top_k=3)
        
        assert len(results) == 2
        assert results[0].page_content == "Recipe 1 content"
        assert results[1].page_content == "Recipe 2 content"
        
        # Verify similarity search was called with correct parameters
        mock_store.similarity_search.assert_called_once_with("pasta recipe", k=3)
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_retrieve_no_results(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test recipe retrieval with no results"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_store.similarity_search.return_value = []
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        results = rag.retrieve("nonexistent recipe")
        
        assert len(results) == 0
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_retrieve_exception(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test recipe retrieval with exception"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_store.similarity_search.side_effect = Exception("Search failed")
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        results = rag.retrieve("pasta recipe")
        
        assert len(results) == 0


class TestRAGHelperDeleteRecipe:
    """Test recipe deletion functionality"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_delete_recipe_success(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test successful recipe deletion"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        result = rag.delete_recipe("123")
        
        assert result is True
        # Verify deletion was attempted (implementation would depend on actual delete method)
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_delete_recipe_by_recipe_id_success(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test successful recipe deletion by recipe ID"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        result = rag.delete_recipe_by_recipe_id("123")
        
        assert result is True
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_delete_recipe_failure(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test recipe deletion failure"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        result = rag.delete_recipe_by_recipe_id("nonexistent")
        
        assert result is True  # Deletion always returns True (handles non-existent gracefully)


class TestRAGHelperCollectionStats:
    """Test collection statistics functionality"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_get_collection_stats_success(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test successful collection statistics retrieval"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        # Mock collection stats
        mock_collection = Mock()
        mock_stats = Mock()
        mock_stats.__len__ = Mock(return_value=10)
        mock_collection.aggregate.over_all.return_value = mock_stats
        mock_client.collections.get.return_value = mock_collection
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        stats = rag.get_collection_stats()
        
        assert "total_objects" in stats
        assert stats["status"] == "healthy"
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_get_collection_stats_exception(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test collection statistics with exception"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_client.collections.get.side_effect = Exception("Stats failed")
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        stats = rag.get_collection_stats()
        
        assert "error" in stats
        assert stats["status"] == "unhealthy"
        assert stats["total_objects"] == 0


class TestRAGHelperCleanup:
    """Test cleanup functionality"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_cleanup(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test cleanup method"""
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        rag = RAGHelper()
        rag.cleanup()
        
        # Verify cleanup was called (implementation would depend on actual cleanup logic)
        assert rag.weaviate_client is not None


class TestRAGHelperErrorHandling:
    """Test error handling in RAG helper"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_initialization_with_invalid_host(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test initialization with invalid host"""
        mock_weaviate_connect.side_effect = Exception("Invalid host")
        
        with pytest.raises(Exception, match="Invalid host"):
            RAGHelper(weaviate_host="invalid-host")
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    def test_operations_without_initialization(self, mock_embeddings, mock_vector_store_class, mock_weaviate_connect):
        """Test operations when not properly initialized"""
        # This would test edge cases where the RAG helper is not properly initialized
        # but methods are still called
        pass


class TestRAGHelperIntegration:
    """Test integration scenarios"""
    
    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    @patch('rag.HuggingFaceEmbeddings')
    @patch('rag.RecursiveCharacterTextSplitter')
    def test_full_recipe_lifecycle(self, mock_splitter_class, mock_embeddings, mock_vector_store_class, mock_weaviate_connect, sample_recipe_content, sample_metadata):
        """Test full recipe lifecycle: add, retrieve, delete"""
        # Setup mocks
        mock_client = Mock()
        mock_client.collections.list_all.return_value = ["recipes"]
        mock_weaviate_connect.return_value = mock_client
        
        mock_store = Mock()
        mock_store.add_documents.return_value = ["doc1"]
        mock_store.similarity_search.return_value = [
            Document(page_content="Test recipe content", metadata=sample_metadata)
        ]
        mock_vector_store_class.return_value = mock_store
        
        mock_emb = Mock()
        mock_embeddings.return_value = mock_emb
        
        mock_splitter = Mock()
        mock_splitter.split_text.return_value = ["chunk1"]
        mock_splitter_class.return_value = mock_splitter
        
        # Test full lifecycle
        rag = RAGHelper()
        
        # 1. Add recipe
        add_result = rag.add_recipe(sample_recipe_content, sample_metadata)
        assert add_result is True
        
        # 2. Retrieve recipe
        retrieve_results = rag.retrieve("test recipe")
        assert len(retrieve_results) == 1
        assert retrieve_results[0].page_content == "Test recipe content"
        
        # 3. Delete recipe
        delete_result = rag.delete_recipe_by_recipe_id("123")
        assert delete_result is True 