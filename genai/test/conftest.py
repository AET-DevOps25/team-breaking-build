import pytest
import os
import sys
from unittest.mock import Mock, patch
from datetime import datetime

# Add the parent directory to Python path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from request_models import (
    RecipeData, RecipeMetadataDTO, RecipeDetailsDTO, 
    RecipeIngredientDTO, RecipeStepDTO, RecipeTagDTO
)
from response_models import ChatResponse, RecipeSuggestionResponse


@pytest.fixture(scope="session")
def test_environment():
    """Set up test environment variables"""
    test_env = {
        'LLM_MODEL': 'test-model',
        'LLM_TEMPERATURE': '0.5',
        'LLM_BASE_URL': 'http://test.com',
        'OPEN_WEBUI_API_KEY': 'test-key',
        'WEAVIATE_HOST': 'localhost',
        'WEAVIATE_PORT': '8080',
        'WEAVIATE_GRPC_PORT': '50051'
    }
    
    # Store original environment
    original_env = {}
    for key in test_env:
        if key in os.environ:
            original_env[key] = os.environ[key]
    
    # Set test environment
    for key, value in test_env.items():
        os.environ[key] = value
    
    yield test_env
    
    # Restore original environment
    for key in test_env:
        if key in original_env:
            os.environ[key] = original_env[key]
        else:
            os.environ.pop(key, None)


@pytest.fixture
def sample_recipe():
    """Create a sample recipe for testing"""
    return RecipeData(
        metadata=RecipeMetadataDTO(
            id=1,
            title="Test Recipe",
            description="A delicious test recipe for testing purposes",
            servingSize=4,
            tags=[
                RecipeTagDTO(id=1, name="test"),
                RecipeTagDTO(id=2, name="delicious")
            ]
        ),
        details=RecipeDetailsDTO(
            servingSize=4,
            recipeIngredients=[
                RecipeIngredientDTO(name="Test Ingredient 1", unit="g", amount=100),
                RecipeIngredientDTO(name="Test Ingredient 2", unit="ml", amount=200),
                RecipeIngredientDTO(name="Test Ingredient 3", unit="tbsp", amount=2)
            ],
            recipeSteps=[
                RecipeStepDTO(order=1, details="Mix all ingredients together"),
                RecipeStepDTO(order=2, details="Cook for 30 minutes on medium heat"),
                RecipeStepDTO(order=3, details="Let it cool for 10 minutes"),
                RecipeStepDTO(order=4, details="Serve hot and enjoy!")
            ]
        )
    )


@pytest.fixture
def sample_recipe_content():
    """Sample recipe content for testing"""
    return """
    Title: Test Recipe
    Description: A delicious test recipe for testing purposes
    
    Ingredients:
    - Test Ingredient 1 (100g)
    - Test Ingredient 2 (200ml)
    - Test Ingredient 3 (2 tbsp)
    
    Steps:
    1. Mix all ingredients together
    2. Cook for 30 minutes on medium heat
    3. Let it cool for 10 minutes
    4. Serve hot and enjoy!
    
    Tags: test, delicious
    Serving Size: 4 people
    """


@pytest.fixture
def sample_metadata():
    """Sample metadata for testing"""
    return {
        "recipe_id": "123",
        "title": "Test Recipe",
        "description": "A delicious test recipe for testing purposes",
        "ingredients": ["Test Ingredient 1", "Test Ingredient 2", "Test Ingredient 3"],
        "steps": [
            "Mix all ingredients together",
            "Cook for 30 minutes on medium heat",
            "Let it cool for 10 minutes",
            "Serve hot and enjoy!"
        ],
        "tags": ["test", "delicious"],
        "serving_size": 4
    }


@pytest.fixture
def mock_llm_response():
    """Mock LLM response for testing"""
    return "This is a test response from the LLM model."


@pytest.fixture
def mock_chat_response():
    """Mock chat response for testing"""
    return ChatResponse(
        reply="Hello! How can I help you with recipes today?",
        sources=["recipe1", "recipe2"],
        recipe_suggestion={
            "title": "Suggested Recipe",
            "ingredients": ["ingredient1", "ingredient2"],
            "steps": ["step1", "step2"]
        },
        timestamp=datetime.now()
    )


@pytest.fixture
def mock_suggestion_response():
    """Mock recipe suggestion response for testing"""
    return RecipeSuggestionResponse(
        suggestion="Here's a great recipe for you based on your preferences!",
        recipe_data={
            "title": "Spicy Pasta",
            "ingredients": ["pasta", "spicy sauce", "vegetables"],
            "steps": ["Cook pasta", "Prepare sauce", "Combine and serve"],
            "serving_size": 2,
            "cooking_time": "30 minutes"
        },
        timestamp=datetime.now()
    )


@pytest.fixture
def mock_weaviate_client():
    """Mock Weaviate client for testing"""
    mock_client = Mock()
    mock_client.collections.list_all.return_value = ["recipes"]
    
    # Mock collection stats
    mock_collection = Mock()
    mock_collection.aggregate.over_all.return_value.with_where.return_value.with_fields.return_value.do.return_value = {
        "total": 10
    }
    mock_client.collections.get.return_value = mock_collection
    
    return mock_client


@pytest.fixture
def mock_vector_store():
    """Mock vector store for testing"""
    mock_store = Mock()
    mock_store.add_documents.return_value = ["doc1", "doc2"]
    mock_store.similarity_search.return_value = []
    return mock_store


@pytest.fixture
def mock_embeddings():
    """Mock embeddings model for testing"""
    mock_emb = Mock()
    mock_emb.embed_query.return_value = [0.1, 0.2, 0.3, 0.4, 0.5]
    mock_emb.embed_documents.return_value = [
        [0.1, 0.2, 0.3, 0.4, 0.5],
        [0.6, 0.7, 0.8, 0.9, 1.0]
    ]
    return mock_emb


@pytest.fixture
def mock_text_splitter():
    """Mock text splitter for testing"""
    mock_splitter = Mock()
    mock_splitter.split_text.return_value = [
        "Chunk 1 of the recipe content",
        "Chunk 2 of the recipe content"
    ]
    return mock_splitter


@pytest.fixture
def sample_search_results():
    """Sample search results for testing"""
    from langchain_core.documents import Document
    
    return [
        Document(
            page_content="Recipe 1: Pasta Carbonara - A classic Italian dish with eggs, cheese, and pancetta",
            metadata={
                "recipe_id": "1",
                "title": "Pasta Carbonara",
                "ingredients": ["pasta", "eggs", "cheese", "pancetta"],
                "tags": ["italian", "pasta"]
            }
        ),
        Document(
            page_content="Recipe 2: Spicy Chicken Curry - A flavorful curry with aromatic spices",
            metadata={
                "recipe_id": "2",
                "title": "Spicy Chicken Curry",
                "ingredients": ["chicken", "curry powder", "coconut milk"],
                "tags": ["indian", "spicy"]
            }
        )
    ]


@pytest.fixture
def mock_llm_instance():
    """Mock LLM instance for testing"""
    mock_llm = Mock()
    mock_llm.invoke.return_value.content = "Test LLM response"
    mock_llm.get_health_status.return_value = {
        "llm": "healthy",
        "vector_store": "healthy"
    }
    mock_llm.cleanup.return_value = None
    return mock_llm


@pytest.fixture
def mock_rag_helper():
    """Mock RAG helper for testing"""
    mock_rag = Mock()
    mock_rag.add_recipe.return_value = True
    mock_rag.delete_recipe_by_recipe_id.return_value = True
    mock_rag.retrieve.return_value = []
    mock_rag.get_collection_stats.return_value = {"count": 10}
    mock_rag.cleanup.return_value = None
    return mock_rag


@pytest.fixture(autouse=True)
def mock_external_dependencies():
    """Automatically mock external dependencies for all tests"""
    with patch('llm.ChatOpenAI'), \
         patch('llm.RAGHelper'), \
         patch('rag.weaviate.connect_to_local'), \
         patch('rag.WeaviateVectorStore'), \
         patch('rag.HuggingFaceEmbeddings'), \
         patch('rag.RecursiveCharacterTextSplitter'):
        yield


@pytest.fixture
def test_recipe_data_list():
    """List of test recipe data for bulk operations"""
    return [
        RecipeData(
            metadata=RecipeMetadataDTO(
                id=1,
                title="Recipe 1",
                description="First test recipe",
                servingSize=2,
                tags=[RecipeTagDTO(name="quick")]
            ),
            details=RecipeDetailsDTO(
                servingSize=2,
                recipeIngredients=[RecipeIngredientDTO(name="Ingredient 1")],
                recipeSteps=[RecipeStepDTO(order=1, details="Step 1")]
            )
        ),
        RecipeData(
            metadata=RecipeMetadataDTO(
                id=2,
                title="Recipe 2",
                description="Second test recipe",
                servingSize=4,
                tags=[RecipeTagDTO(name="healthy")]
            ),
            details=RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[RecipeIngredientDTO(name="Ingredient 2")],
                recipeSteps=[RecipeStepDTO(order=1, details="Step 1")]
            )
        ),
        RecipeData(
            metadata=RecipeMetadataDTO(
                id=3,
                title="Recipe 3",
                description="Third test recipe",
                servingSize=6,
                tags=[RecipeTagDTO(name="dessert")]
            ),
            details=RecipeDetailsDTO(
                servingSize=6,
                recipeIngredients=[RecipeIngredientDTO(name="Ingredient 3")],
                recipeSteps=[RecipeStepDTO(order=1, details="Step 1")]
            )
        )
    ]


@pytest.fixture
def sample_chat_messages():
    """Sample chat messages for testing different scenarios"""
    return {
        "simple_greeting": "Hello, how are you?",
        "recipe_search": "I want to make pasta for dinner",
        "recipe_creation": "Create a recipe for chocolate cake",
        "ingredient_query": "What can I make with chicken and rice?",
        "cooking_advice": "How do I cook the perfect steak?",
        "dietary_restriction": "I need a vegetarian recipe without dairy",
        "complex_request": "I want to make something spicy and quick for 4 people using ingredients I have: tomatoes, onions, and chicken"
    }


@pytest.fixture
def sample_queries():
    """Sample queries for recipe suggestions"""
    return {
        "simple": "I want something spicy",
        "complex": "I need a healthy vegetarian dinner for 2 people that takes less than 30 minutes",
        "ingredient_based": "What can I make with chicken, rice, and vegetables?",
        "cuisine_specific": "I want an Italian recipe for tonight",
        "dietary": "I need a gluten-free dessert recipe",
        "occasion": "I want to make something special for a birthday party"
    }


# Pytest configuration
def pytest_configure(config):
    """Configure pytest for the genai service tests"""
    # Add custom markers
    config.addinivalue_line(
        "markers", "integration: marks tests as integration tests"
    )
    config.addinivalue_line(
        "markers", "unit: marks tests as unit tests"
    )
    config.addinivalue_line(
        "markers", "slow: marks tests as slow running"
    )


def pytest_collection_modifyitems(config, items):
    """Modify test collection to add default markers"""
    for item in items:
        # Add unit marker by default if no marker is specified
        if not any(item.iter_markers()):
            item.add_marker(pytest.mark.unit)
        
        # Add slow marker for tests that might take longer
        if "llm" in item.name.lower() or "rag" in item.name.lower():
            item.add_marker(pytest.mark.slow) 


@pytest.fixture
def client():
    """Create a test client for the FastAPI app"""
    from fastapi.testclient import TestClient
    from main import app
    return TestClient(app) 