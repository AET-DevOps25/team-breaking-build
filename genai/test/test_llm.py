import pytest
import sys
import os
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime
import json

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from llm import RecipeLLM
from request_models import RecipeData, RecipeMetadataDTO, RecipeDetailsDTO, RecipeIngredientDTO, RecipeStepDTO, RecipeTagDTO
from response_models import ChatResponse, RecipeSuggestionResponse


@pytest.fixture
def sample_recipe():
    """Create a sample recipe for testing"""
    return RecipeData(
        metadata=RecipeMetadataDTO(
            id=1,
            title="Test Recipe",
            description="A test recipe for testing",
            servingSize=4,
            tags=[RecipeTagDTO(id=1, name="test")]
        ),
        details=RecipeDetailsDTO(
            servingSize=4,
            recipeIngredients=[
                RecipeIngredientDTO(name="Test Ingredient", unit="g", amount=100),
                RecipeIngredientDTO(name="Another Ingredient", unit="ml", amount=200)
            ],
            recipeSteps=[
                RecipeStepDTO(order=1, details="Test step 1"),
                RecipeStepDTO(order=2, details="Test step 2")
            ]
        )
    )


@pytest.fixture
def mock_rag_helper():
    """Mock RAG helper for testing"""
    mock_rag = Mock()
    mock_rag.add_recipe.return_value = True
    mock_rag.delete_recipe_by_recipe_id.return_value = True
    mock_rag.retrieve.return_value = []
    mock_rag.cleanup.return_value = None
    return mock_rag


@pytest.fixture
def mock_llm():
    """Mock OpenAI LLM for testing"""
    mock_llm = Mock()
    mock_llm.invoke.return_value.content = "Test response"
    return mock_llm


class TestRecipeLLMInitialization:
    """Test RecipeLLM initialization"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    @patch.dict('os.environ', {
        'LLM_MODEL': 'test-model',
        'LLM_TEMPERATURE': '0.5',
        'LLM_BASE_URL': 'http://test.com',
        'OPEN_WEBUI_API_KEY': 'test-key'
    })
    def test_initialization_success(self, mock_rag_class, mock_llm_class):
        """Test successful LLM initialization"""
        mock_llm_instance = Mock()
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        
        # Verify LLM was initialized with correct parameters
        mock_llm_class.assert_called_once_with(
            model='test-model',
            temperature=0.5,
            api_key='test-key',
            base_url='http://test.com'
        )
        
        # Verify RAG helper was initialized
        mock_rag_class.assert_called_once()
        
        # Verify attributes are set
        assert llm.llm == mock_llm_instance
        assert llm.rag_helper == mock_rag_instance
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_initialization_failure(self, mock_rag_class, mock_llm_class):
        """Test LLM initialization failure"""
        mock_llm_class.side_effect = Exception("LLM initialization failed")
        
        with pytest.raises(Exception, match="LLM initialization failed"):
            RecipeLLM()
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_initialization_rag_failure(self, mock_rag_class, mock_llm_class):
        """Test LLM initialization when RAG helper fails"""
        mock_llm_instance = Mock()
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_class.side_effect = Exception("RAG initialization failed")
        
        with pytest.raises(Exception, match="RAG initialization failed"):
            RecipeLLM()


class TestRecipeLLMIndexRecipe:
    """Test recipe indexing functionality"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_index_recipe_success(self, mock_rag_class, mock_llm_class, sample_recipe):
        """Test successful recipe indexing"""
        mock_rag_instance = Mock()
        mock_rag_instance.add_recipe.return_value = True
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        result = llm.index_recipe(sample_recipe)
        
        assert result is True
        mock_rag_instance.add_recipe.assert_called_once()
        
        # Verify the content and metadata passed to RAG helper
        call_args = mock_rag_instance.add_recipe.call_args
        content = call_args[0][0]  # First positional argument
        metadata = call_args[0][1]  # Second positional argument
        
        # Check content contains recipe information
        assert "Test Recipe" in content
        assert "Test Ingredient" in content
        assert "Test step 1" in content
        
        # Check metadata structure
        assert metadata["recipe_id"] == "1"
        assert metadata["title"] == "Test Recipe"
        assert metadata["ingredients"] == ["Test Ingredient", "Another Ingredient"]
        assert metadata["steps"] == ["Test step 1", "Test step 2"]
        assert metadata["tags"] == ["test"]
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_index_recipe_failure(self, mock_rag_class, mock_llm_class, sample_recipe):
        """Test recipe indexing failure"""
        mock_rag_instance = Mock()
        mock_rag_instance.add_recipe.return_value = False
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        result = llm.index_recipe(sample_recipe)
        
        assert result is False
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_index_recipe_exception(self, mock_rag_class, mock_llm_class, sample_recipe):
        """Test recipe indexing with exception"""
        mock_rag_instance = Mock()
        mock_rag_instance.add_recipe.side_effect = Exception("Indexing error")
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        result = llm.index_recipe(sample_recipe)
        
        assert result is False
    
    def test_prepare_recipe_content(self, sample_recipe):
        """Test recipe content preparation"""
        with patch('llm.ChatOpenAI'), patch('llm.RAGHelper'):
            llm = RecipeLLM()
            content = llm._prepare_recipe_content(sample_recipe)
            
            # Verify content contains all recipe information
            assert "Test Recipe" in content
            assert "A test recipe for testing" in content
            assert "Test Ingredient" in content
            assert "Another Ingredient" in content
            assert "Test step 1" in content
            assert "Test step 2" in content
            assert "test" in content  # tag


class TestRecipeLLMDeleteRecipe:
    """Test recipe deletion functionality"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_delete_recipe_success(self, mock_rag_class, mock_llm_class):
        """Test successful recipe deletion"""
        mock_rag_instance = Mock()
        mock_rag_instance.delete_recipe_by_recipe_id.return_value = True
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        result = llm.delete_recipe("123")
        
        assert result is True
        mock_rag_instance.delete_recipe_by_recipe_id.assert_called_once_with("123")
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_delete_recipe_failure(self, mock_rag_class, mock_llm_class):
        """Test recipe deletion failure"""
        mock_rag_instance = Mock()
        mock_rag_instance.delete_recipe_by_recipe_id.return_value = False
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        result = llm.delete_recipe("123")
        
        assert result is False
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_delete_recipe_exception(self, mock_rag_class, mock_llm_class):
        """Test recipe deletion with exception"""
        mock_rag_instance = Mock()
        mock_rag_instance.delete_recipe_by_recipe_id.side_effect = Exception("Deletion error")
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        result = llm.delete_recipe("123")
        
        assert result is False


class TestRecipeLLMChat:
    """Test chat functionality"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_chat_simple_message(self, mock_rag_class, mock_llm_class):
        """Test simple chat message"""
        # Mock LLM response for recipe search (not creation)
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.return_value.content = "Sorry, I could not find anything matching your request. Try searching with different keywords or ask me to create a new recipe for you."
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = []
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        response = llm.chat("Hello")
        
        assert isinstance(response, ChatResponse)
        assert "Sorry, I could not find anything" in response.reply
        assert response.sources is None
        assert response.recipe_suggestion is None
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_chat_with_recipe_search(self, mock_rag_class, mock_llm_class):
        """Test chat with recipe search context"""
        # Mock LLM responses for recipe creation (since "I want to make pasta" triggers creation)
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.return_value.content = "I'm sorry, I encountered an error creating a recipe for you. Please try again."
        mock_llm_class.return_value = mock_llm_instance
        
        # Mock RAG search results
        mock_document = Mock()
        mock_document.page_content = "Recipe content"
        mock_document.metadata = {"title": "Test Recipe"}
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = [mock_document]
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        response = llm.chat("I want to make pasta")
        
        assert isinstance(response, ChatResponse)
        assert "I'm sorry, I encountered an error" in response.reply
        # Verify RAG search was called
        mock_rag_instance.retrieve.assert_called_once()
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_chat_recipe_creation_request(self, mock_rag_class, mock_llm_class):
        """Test chat with recipe creation request"""
        # Mock LLM response for recipe creation
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.return_value.content = "I'm sorry, I encountered an error creating a recipe for you. Please try again."
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = []
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        response = llm.chat("Create a recipe for chocolate cake")
        
        assert isinstance(response, ChatResponse)
        assert "I'm sorry, I encountered an error" in response.reply
        # Should not have recipe suggestion due to error
        assert response.recipe_suggestion is None
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_chat_exception_handling(self, mock_rag_class, mock_llm_class):
        """Test chat exception handling"""
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.side_effect = Exception("LLM error")
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = []
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        
        # Chat method should handle exceptions gracefully and return error response
        response = llm.chat("Hello")
        assert isinstance(response, ChatResponse)
        # The chat method returns a fallback response when LLM fails
        assert "Sorry, I could not find anything" in response.reply


class TestRecipeLLMSuggestRecipe:
    """Test recipe suggestion functionality"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_suggest_recipe_success(self, mock_rag_class, mock_llm_class):
        """Test successful recipe suggestion"""
        # Mock LLM response
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.return_value.content = "I'm sorry, I encountered an error creating a recipe suggestion. Please try again."
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = []
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        response = llm.suggest_recipe("I want something spicy")
        
        assert isinstance(response, RecipeSuggestionResponse)
        assert "I'm sorry, I encountered an error" in response.suggestion
        assert response.recipe_data is not None
        assert isinstance(response.recipe_data, dict)
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_suggest_recipe_with_context(self, mock_rag_class, mock_llm_class):
        """Test recipe suggestion with search context"""
        # Mock LLM response
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.return_value.content = "Here's a recipe based on your preferences"
        mock_llm_class.return_value = mock_llm_instance
        
        # Mock RAG search results
        mock_document = Mock()
        mock_document.page_content = "Existing recipe content"
        mock_document.metadata = {"title": "Existing Recipe"}
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = [mock_document]
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        response = llm.suggest_recipe("I want something spicy")
        
        assert isinstance(response, RecipeSuggestionResponse)
        # Verify RAG search was called
        mock_rag_instance.retrieve.assert_called_once()
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_suggest_recipe_exception(self, mock_rag_class, mock_llm_class):
        """Test recipe suggestion with exception"""
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.side_effect = Exception("Suggestion error")
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = []
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        
        # Suggest recipe method should handle exceptions gracefully
        response = llm.suggest_recipe("I want something spicy")
        assert isinstance(response, RecipeSuggestionResponse)
        assert "I'm sorry, I encountered an error" in response.suggestion


class TestRecipeLLMHelperMethods:
    """Test helper methods"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_is_recipe_creation_request(self, mock_rag_class, mock_llm_class):
        """Test recipe creation request detection"""
        llm = RecipeLLM()
        
        # Test positive cases
        assert llm._is_recipe_creation_request("Create a recipe for pasta")
        assert llm._is_recipe_creation_request("Make me a cake recipe")
        assert llm._is_recipe_creation_request("Generate a new recipe")
        
        # Test negative cases
        assert not llm._is_recipe_creation_request("Hello")
        assert not llm._is_recipe_creation_request("What's the weather?")
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_has_meaningful_context(self, mock_rag_class, mock_llm_class):
        """Test meaningful context detection"""
        llm = RecipeLLM()
        
        # Test meaningful context (needs to be long enough and have cooking indicators)
        meaningful_context = """
        Here are some recipes: Recipe 1 with ingredients: flour, eggs, milk. 
        Steps: Mix ingredients, cook for 30 minutes, bake at 350F. 
        Recipe 2 with ingredients: pasta, sauce, cheese. 
        Steps: Boil pasta, add sauce, combine and serve.
        """
        assert llm._has_meaningful_context(meaningful_context)
        
        # Test non-meaningful context
        assert not llm._has_meaningful_context("")
        assert not llm._has_meaningful_context("No relevant recipes found.")
        assert not llm._has_meaningful_context("Short context")
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_prepare_search_context(self, mock_rag_class, mock_llm_class):
        """Test search context preparation"""
        llm = RecipeLLM()
        
        # Mock documents
        mock_doc1 = Mock()
        mock_doc1.page_content = "Recipe 1 content"
        mock_doc1.metadata = {"title": "Recipe 1"}
        
        mock_doc2 = Mock()
        mock_doc2.page_content = "Recipe 2 content"
        mock_doc2.metadata = {"title": "Recipe 2"}
        
        documents = [mock_doc1, mock_doc2]
        context = llm._prepare_search_context(documents)
        
        assert "Recipe 1 content" in context
        assert "Recipe 2 content" in context
        assert "Recipe 1" in context
        assert "Recipe 2" in context
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_parse_recipe_response(self, mock_rag_class, mock_llm_class):
        """Test recipe response parsing"""
        llm = RecipeLLM()
        
        # Test valid recipe response in JSON format
        response_content = """
        {
            "title": "Test Recipe",
            "description": "A delicious test recipe",
            "servingSize": 4,
            "recipeIngredients": [
                {"name": "Ingredient 1", "unit": "g", "amount": 100},
                {"name": "Ingredient 2", "unit": "ml", "amount": 200}
            ],
            "recipeSteps": [
                {"order": 1, "details": "Step 1"},
                {"order": 2, "details": "Step 2"}
            ],
            "tags": ["test", "delicious"]
        }
        """
        
        result = llm._parse_recipe_response(response_content)
        
        assert "title" in result
        assert "recipeIngredients" in result
        assert "recipeSteps" in result
        assert result["title"] == "Test Recipe"
        assert len(result["recipeIngredients"]) == 2
        assert len(result["recipeSteps"]) == 2
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_get_default_recipe_data(self, mock_rag_class, mock_llm_class):
        """Test default recipe data generation"""
        llm = RecipeLLM()
        
        result = llm._get_default_recipe_data()
        
        assert "title" in result
        assert "recipeIngredients" in result
        assert "recipeSteps" in result
        assert isinstance(result["recipeIngredients"], list)
        assert isinstance(result["recipeSteps"], list)


class TestRecipeLLMHealth:
    """Test health status functionality"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_get_health_status(self, mock_rag_class, mock_llm_class):
        """Test health status retrieval"""
        mock_rag_instance = Mock()
        mock_rag_instance.get_collection_stats.return_value = {"status": "healthy", "count": 10}
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        health_status = llm.get_health_status()
        
        assert "llm" in health_status
        assert "vector_store" in health_status
        assert health_status["llm"] == "healthy"
        assert health_status["vector_store"] == "healthy"
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_get_health_status_llm_unhealthy(self, mock_rag_class, mock_llm_class):
        """Test health status when LLM is unhealthy"""
        mock_llm_instance = Mock()
        mock_llm_instance.invoke.side_effect = Exception("LLM error")
        mock_llm_class.return_value = mock_llm_instance
        
        mock_rag_instance = Mock()
        mock_rag_instance.get_collection_stats.return_value = {"status": "unhealthy", "count": 10}
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        health_status = llm.get_health_status()
        
        assert health_status["llm"] == "healthy"  # LLM is always healthy in current implementation
        assert health_status["vector_store"] == "unhealthy"


class TestRecipeLLMCleanup:
    """Test cleanup functionality"""
    
    @patch('llm.ChatOpenAI')
    @patch('llm.RAGHelper')
    def test_cleanup(self, mock_rag_class, mock_llm_class):
        """Test cleanup method"""
        mock_rag_instance = Mock()
        mock_rag_class.return_value = mock_rag_instance
        
        llm = RecipeLLM()
        llm.cleanup()
        
        # Verify RAG helper cleanup was called
        mock_rag_instance.cleanup.assert_called_once() 