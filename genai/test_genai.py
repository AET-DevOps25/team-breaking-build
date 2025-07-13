import pytest
import json
from unittest.mock import Mock, patch, MagicMock, AsyncMock
from fastapi.testclient import TestClient
from datetime import datetime
import asyncio
import os

# Set environment variables before importing application modules
os.environ["LLM_MODEL"] = "test-model"
os.environ["LLM_TEMPERATURE"] = "0.7"
os.environ["LLM_BASE_URL"] = "http://test-llm"
os.environ["OPEN_WEBUI_API_KEY"] = "test-api-key"
os.environ["WEAVIATE_HOST"] = "test-weaviate"
os.environ["WEAVIATE_PORT"] = "8080"
os.environ["WEAVIATE_GRPC_PORT"] = "50051"

# Import application modules after setting environment variables
from main import app
from llm import RecipeLLM
from rag import RAGHelper
from request_models import ChatRequest, RecipeIndexRequest, RecipeDeleteRequest, RecipeSuggestionRequest, RecipeData
from response_models import ChatResponse, RecipeIndexResponse, RecipeDeleteResponse, RecipeSuggestionResponse, HealthResponse


class TestGenAIService:
    """Test suite for GenAI service with mocked LLM and Weaviate"""

    @pytest.fixture
    def client(self):
        """Create test client"""
        with TestClient(app) as client:
            yield client

    @pytest.fixture
    def mock_llm_instance(self):
        """Mock LLM instance"""
        mock_llm = Mock(spec=RecipeLLM)
        mock_llm.chat.return_value = ChatResponse(
            reply="Test response",
            sources=["test-source"],
            recipe_suggestion=None,
            timestamp=datetime.now()
        )
        mock_llm.index_recipe.return_value = True
        mock_llm.delete_recipe.return_value = True
        mock_llm.suggest_recipe.return_value = RecipeSuggestionResponse(
            suggestion="Test suggestion",
            recipe_data={"title": "Test Recipe", "description": "Test description"},
            timestamp=datetime.now()
        )
        mock_llm.get_health_status.return_value = {
            "llm_service": "healthy",
            "vector_store": "healthy"
        }
        mock_llm.cleanup.return_value = None
        return mock_llm

    @pytest.fixture
    def mock_rag_helper(self):
        """Mock RAG helper"""
        mock_rag = Mock(spec=RAGHelper)
        mock_rag.retrieve.return_value = [
            {"content": "Test document", "metadata": {"recipe_id": "test-1"}}
        ]
        mock_rag.add_document.return_value = True
        mock_rag.delete_document.return_value = True
        mock_rag.get_health_status.return_value = "healthy"
        return mock_rag

    @pytest.fixture
    def sample_recipe_data(self):
        """Sample recipe data for testing"""
        return {
            "metadata": {
                "id": 1,
                "title": "Test Recipe",
                "description": "A test recipe",
                "userId": "user-123"
            },
            "details": {
                "servingSize": 4,
                "recipeIngredients": [
                    {"name": "flour", "amount": 2, "unit": "cups"},
                    {"name": "sugar", "amount": 1, "unit": "cup"}
                ],
                "recipeSteps": [
                    {"order": 1, "details": "Mix ingredients"},
                    {"order": 2, "details": "Bake at 350°F"}
                ]
            }
        }

    def test_root_endpoint(self, client):
        """Test root endpoint"""
        response = client.get("/genai")
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "GenAI Recipe Service"
        assert data["version"] == "1.0.0"
        assert data["status"] == "running"
        assert "request_id" in data

    @patch('main.llm_instance')
    def test_health_endpoint_healthy(self, mock_llm_instance, client):
        """Test health endpoint when service is healthy"""
        mock_llm_instance.get_health_status.return_value = {
            "llm_service": "healthy",
            "vector_store": "healthy"
        }
        
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["services"]["llm_service"] == "healthy"
        assert data["services"]["vector_store"] == "healthy"

    @patch('main.llm_instance')
    def test_health_endpoint_unhealthy(self, mock_llm_instance, client):
        """Test health endpoint when service is unhealthy"""
        mock_llm_instance.get_health_status.return_value = {
            "llm_service": "unhealthy",
            "vector_store": "healthy"
        }
        
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "unhealthy"
        assert data["services"]["llm_service"] == "unhealthy"

    def test_health_endpoint_no_llm_instance(self, client):
        """Test health endpoint when LLM instance is not initialized"""
        with patch('main.llm_instance', None):
            response = client.get("/health")
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "unhealthy"
            assert "error" in data["services"]

    def test_metrics_endpoint(self, client):
        """Test metrics endpoint"""
        response = client.get("/metrics")
        assert response.status_code == 200
        assert "text/plain" in response.headers["content-type"]
        # Check for some expected metrics
        content = response.text
        assert "genai_service_health" in content
        assert "genai_requests_total" in content

    @patch('main.llm_instance')
    def test_chat_endpoint_success(self, mock_llm_instance, client):
        """Test chat endpoint with successful response"""
        mock_llm_instance.chat.return_value = ChatResponse(
            reply="Test chat response",
            sources=["source1", "source2"],
            recipe_suggestion={"title": "Suggested Recipe"},
            timestamp=datetime.now()
        )
        
        request_data = {"message": "How do I make pasta?"}
        response = client.post("/genai/chat", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        assert data["reply"] == "Test chat response"
        assert data["sources"] == ["source1", "source2"]
        assert data["recipe_suggestion"]["title"] == "Suggested Recipe"
        
        # Verify LLM was called with correct message
        mock_llm_instance.chat.assert_called_once_with("How do I make pasta?")

    @patch('main.llm_instance')
    def test_chat_endpoint_no_llm_instance(self, mock_llm_instance, client):
        """Test chat endpoint when LLM instance is not available"""
        with patch('main.llm_instance', None):
            request_data = {"message": "How do I make pasta?"}
            response = client.post("/genai/chat", json=request_data)
            
            assert response.status_code == 500
            data = response.json()
            assert "LLM service not initialized" in data["detail"]

    @patch('main.llm_instance')
    def test_chat_endpoint_llm_error(self, mock_llm_instance, client):
        """Test chat endpoint when LLM throws an error"""
        mock_llm_instance.chat.side_effect = Exception("LLM error")
        
        request_data = {"message": "How do I make pasta?"}
        response = client.post("/genai/chat", json=request_data)
        
        assert response.status_code == 500

    @patch('main.llm_instance')
    def test_index_recipe_endpoint_success(self, mock_llm_instance, client, sample_recipe_data):
        """Test recipe indexing endpoint with successful response"""
        mock_llm_instance.index_recipe.return_value = True
        
        response = client.post("/genai/vector/index", json={"recipe": sample_recipe_data})
        
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Recipe indexed successfully"
        assert data["recipe_id"] == 1
        
        # Verify LLM was called with correct recipe data
        mock_llm_instance.index_recipe.assert_called_once()

    @patch('main.llm_instance')
    def test_index_recipe_endpoint_failure(self, mock_llm_instance, client, sample_recipe_data):
        """Test recipe indexing endpoint with failure"""
        mock_llm_instance.index_recipe.return_value = False
        
        response = client.post("/genai/vector/index", json={"recipe": sample_recipe_data})
        
        assert response.status_code == 500
        data = response.json()
        assert "Failed to index recipe" in data["detail"]

    @patch('main.llm_instance')
    def test_index_recipe_endpoint_no_llm_instance(self, mock_llm_instance, client, sample_recipe_data):
        """Test recipe indexing endpoint when LLM instance is not available"""
        with patch('main.llm_instance', None):
            response = client.post("/genai/vector/index", json={"recipe": sample_recipe_data})
            
            assert response.status_code == 500
            data = response.json()
            assert "LLM service not initialized" in data["detail"]

    @patch('main.llm_instance')
    def test_delete_recipe_endpoint_success(self, mock_llm_instance, client):
        """Test recipe deletion endpoint with successful response"""
        mock_llm_instance.delete_recipe.return_value = True
        
        response = client.delete("/genai/vector/test-recipe-123")
        
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Recipe deleted successfully"
        assert data["recipe_id"] == "test-recipe-123"
        
        # Verify LLM was called with correct recipe ID
        mock_llm_instance.delete_recipe.assert_called_once_with("test-recipe-123")

    @patch('main.llm_instance')
    def test_delete_recipe_endpoint_failure(self, mock_llm_instance, client):
        """Test recipe deletion endpoint with failure"""
        mock_llm_instance.delete_recipe.return_value = False
        
        response = client.delete("/genai/vector/test-recipe-123")
        
        assert response.status_code == 500
        data = response.json()
        assert "Failed to delete recipe" in data["detail"]

    @patch('main.llm_instance')
    def test_delete_recipe_endpoint_no_llm_instance(self, mock_llm_instance, client):
        """Test recipe deletion endpoint when LLM instance is not available"""
        with patch('main.llm_instance', None):
            response = client.delete("/genai/vector/test-recipe-123")
            
            assert response.status_code == 500
            data = response.json()
            assert "LLM service not initialized" in data["detail"]

    @patch('main.llm_instance')
    def test_suggest_recipe_endpoint_success(self, mock_llm_instance, client):
        """Test recipe suggestion endpoint with successful response"""
        mock_llm_instance.suggest_recipe.return_value = RecipeSuggestionResponse(
            suggestion="Here's a great pasta recipe",
            recipe_data={
                "title": "Spaghetti Carbonara",
                "description": "Classic Italian pasta dish",
                "servingSize": 4,
                "recipeIngredients": [
                    {"name": "spaghetti", "amount": 400, "unit": "grams"}
                ],
                "recipeSteps": [
                    {"order": 1, "details": "Cook pasta"}
                ]
            },
            timestamp=datetime.now()
        )
        
        request_data = {"query": "I want to make pasta"}
        response = client.post("/genai/vector/suggest", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        assert data["suggestion"] == "Here's a great pasta recipe"
        assert data["recipe_data"]["title"] == "Spaghetti Carbonara"
        
        # Verify LLM was called with correct query
        mock_llm_instance.suggest_recipe.assert_called_once_with("I want to make pasta")

    @patch('main.llm_instance')
    def test_suggest_recipe_endpoint_no_llm_instance(self, mock_llm_instance, client):
        """Test recipe suggestion endpoint when LLM instance is not available"""
        with patch('main.llm_instance', None):
            request_data = {"query": "I want to make pasta"}
            response = client.post("/genai/vector/suggest", json=request_data)
            
            assert response.status_code == 500
            data = response.json()
            assert "LLM service not initialized" in data["detail"]

    @patch('main.llm_instance')
    def test_suggest_recipe_endpoint_llm_error(self, mock_llm_instance, client):
        """Test recipe suggestion endpoint when LLM throws an error"""
        mock_llm_instance.suggest_recipe.side_effect = Exception("LLM error")
        
        request_data = {"query": "I want to make pasta"}
        response = client.post("/genai/vector/suggest", json=request_data)
        
        assert response.status_code == 500

    def test_request_id_middleware(self, client):
        """Test that request ID middleware is working"""
        response = client.get("/genai")
        assert response.status_code == 200
        data = response.json()
        assert "request_id" in data
        assert data["request_id"] is not None

    def test_invalid_json_request(self, client):
        """Test handling of invalid JSON in request"""
        response = client.post("/genai/chat", data="invalid json")
        assert response.status_code == 422  # Unprocessable Entity

    def test_missing_required_fields(self, client):
        """Test handling of missing required fields"""
        # Chat without message
        response = client.post("/genai/chat", json={})
        assert response.status_code == 422

        # Index without recipe
        response = client.post("/genai/vector/index", json={})
        assert response.status_code == 422

        # Suggest without query
        response = client.post("/genai/vector/suggest", json={})
        assert response.status_code == 422


class TestRecipeLLMUnit:
    """Unit tests for RecipeLLM class with mocked dependencies"""

    @pytest.fixture
    def mock_openai_client(self):
        """Mock OpenAI client"""
        mock_client = Mock()
        mock_client.chat.completions.create.return_value.choices = [
            Mock(message=Mock(content="Test response"))
        ]
        return mock_client

    @pytest.fixture
    def mock_rag_helper(self):
        """Mock RAG helper"""
        mock_rag = Mock()
        mock_rag.retrieve.return_value = [
            {"content": "Test document", "metadata": {"recipe_id": "test-1"}}
        ]
        mock_rag.add_document.return_value = True
        mock_rag.delete_document.return_value = True
        mock_rag.get_health_status.return_value = "healthy"
        return mock_rag

    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_recipe_llm_initialization(self, mock_rag_class, mock_openai_class):
        """Test RecipeLLM initialization with mocked dependencies"""
        mock_rag_class.return_value = Mock()
        mock_openai_class.return_value = Mock()
        
        llm = RecipeLLM()
        
        assert llm is not None
        mock_openai_class.assert_called_once()
        mock_rag_class.assert_called_once()

    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_recipe_llm_health_status(self, mock_rag_class, mock_openai_class):
        """Test RecipeLLM health status check"""
        mock_rag = Mock()
        mock_rag.get_health_status.return_value = "healthy"
        mock_rag_class.return_value = mock_rag
        mock_openai_class.return_value = Mock()
        
        llm = RecipeLLM()
        health_status = llm.get_health_status()
        
        assert "llm_service" in health_status
        assert "vector_store" in health_status
        assert health_status["vector_store"] == "healthy"


class TestRAGHelperUnit:
    """Unit tests for RAGHelper class with mocked Weaviate"""

    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    def test_rag_helper_initialization(self, mock_vector_store, mock_weaviate_connect):
        """Test RAGHelper initialization with mocked Weaviate"""
        mock_client = Mock()
        mock_weaviate_connect.return_value = mock_client
        mock_vector_store.return_value = Mock()
        
        rag_helper = RAGHelper()
        
        assert rag_helper is not None
        mock_weaviate_connect.assert_called_once()
        mock_vector_store.assert_called_once()

    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    def test_rag_helper_health_status(self, mock_vector_store, mock_weaviate_connect):
        """Test RAGHelper health status check"""
        mock_client = Mock()
        mock_client.is_ready.return_value = True
        mock_weaviate_connect.return_value = mock_client
        mock_vector_store.return_value = Mock()
        
        rag_helper = RAGHelper()
        health_status = rag_helper.get_health_status()
        
        assert health_status == "healthy"

    @patch('rag.weaviate.connect_to_local')
    @patch('rag.WeaviateVectorStore')
    def test_rag_helper_connection_error(self, mock_vector_store, mock_weaviate_connect):
        """Test RAGHelper handling of connection errors"""
        mock_weaviate_connect.side_effect = Exception("Connection failed")
        
        with pytest.raises(Exception):
            RAGHelper()


# Test configuration for pytest
if __name__ == "__main__":
    pytest.main([__file__, "-v"]) 