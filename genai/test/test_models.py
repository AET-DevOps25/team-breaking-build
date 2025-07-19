import pytest
from datetime import datetime
from pydantic import ValidationError

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from request_models import (
    RecipeIngredientDTO, RecipeStepDTO, RecipeTagDTO, RecipeMetadataDTO,
    RecipeDetailsDTO, RecipeData, ChatRequest, RecipeIndexRequest,
    RecipeDeleteRequest, RecipeSuggestionRequest
)
from response_models import (
    ChatResponse, RecipeIndexResponse, RecipeDeleteResponse,
    RecipeSuggestionResponse, HealthResponse
)


class TestRecipeIngredientDTO:
    """Test RecipeIngredientDTO model"""
    
    def test_valid_ingredient(self):
        """Test valid ingredient creation"""
        ingredient = RecipeIngredientDTO(
            name="Flour",
            unit="g",
            amount=200.0
        )
        
        assert ingredient.name == "Flour"
        assert ingredient.unit == "g"
        assert ingredient.amount == 200.0
    
    def test_ingredient_without_optional_fields(self):
        """Test ingredient creation without optional fields"""
        ingredient = RecipeIngredientDTO(name="Salt")
        
        assert ingredient.name == "Salt"
        assert ingredient.unit is None
        assert ingredient.amount is None
    
    def test_ingredient_validation(self):
        """Test ingredient validation"""
        # Name is required
        with pytest.raises(ValidationError):
            RecipeIngredientDTO()
        
        # Empty name is actually allowed by the current model
        # This test documents the current behavior
        ingredient = RecipeIngredientDTO(name="")
        assert ingredient.name == ""


class TestRecipeStepDTO:
    """Test RecipeStepDTO model"""
    
    def test_valid_step(self):
        """Test valid step creation"""
        step = RecipeStepDTO(
            order=1,
            details="Mix flour and water"
        )
        
        assert step.order == 1
        assert step.details == "Mix flour and water"
    
    def test_step_validation(self):
        """Test step validation"""
        # Both fields are required
        with pytest.raises(ValidationError):
            RecipeStepDTO()
        
        with pytest.raises(ValidationError):
            RecipeStepDTO(order=1)
        
        with pytest.raises(ValidationError):
            RecipeStepDTO(details="Mix ingredients")
        
        # Order can be negative in current model (no validation)
        step = RecipeStepDTO(order=-1, details="Test")
        assert step.order == -1
        
        # Empty details are allowed in current model
        step = RecipeStepDTO(order=1, details="")
        assert step.details == ""


class TestRecipeTagDTO:
    """Test RecipeTagDTO model"""
    
    def test_valid_tag(self):
        """Test valid tag creation"""
        tag = RecipeTagDTO(
            id=1,
            name="Italian"
        )
        
        assert tag.id == 1
        assert tag.name == "Italian"
    
    def test_tag_without_id(self):
        """Test tag creation without ID"""
        tag = RecipeTagDTO(name="Spicy")
        
        assert tag.id is None
        assert tag.name == "Spicy"
    
    def test_tag_validation(self):
        """Test tag validation"""
        # Name is required
        with pytest.raises(ValidationError):
            RecipeTagDTO()
        
        # Empty name is allowed in current model
        tag = RecipeTagDTO(name="")
        assert tag.name == ""


class TestRecipeMetadataDTO:
    """Test RecipeMetadataDTO model"""
    
    def test_valid_metadata(self):
        """Test valid metadata creation"""
        metadata = RecipeMetadataDTO(
            id=1,
            title="Pasta Carbonara",
            description="A classic Italian pasta dish",
            servingSize=4,
            tags=[RecipeTagDTO(id=1, name="Italian"), RecipeTagDTO(name="Pasta")]
        )
        
        assert metadata.id == 1
        assert metadata.title == "Pasta Carbonara"
        assert metadata.description == "A classic Italian pasta dish"
        assert metadata.servingSize == 4
        assert len(metadata.tags) == 2
        assert metadata.tags[0].name == "Italian"
        assert metadata.tags[1].name == "Pasta"
    
    def test_metadata_without_optional_fields(self):
        """Test metadata creation without optional fields"""
        metadata = RecipeMetadataDTO(
            title="Simple Recipe",
            tags=[]
        )
        
        assert metadata.id is None
        assert metadata.title == "Simple Recipe"
        assert metadata.description is None
        assert metadata.servingSize is None
        assert metadata.tags == []
    
    def test_metadata_validation(self):
        """Test metadata validation"""
        # Title is required
        with pytest.raises(ValidationError):
            RecipeMetadataDTO()
        
        # Empty title is allowed in current model
        metadata = RecipeMetadataDTO(title="")
        assert metadata.title == ""


class TestRecipeDetailsDTO:
    """Test RecipeDetailsDTO model"""
    
    def test_valid_details(self):
        """Test valid details creation"""
        details = RecipeDetailsDTO(
            servingSize=4,
            recipeIngredients=[
                RecipeIngredientDTO(name="Flour", unit="g", amount=200),
                RecipeIngredientDTO(name="Water", unit="ml", amount=100)
            ],
            recipeSteps=[
                RecipeStepDTO(order=1, details="Mix flour and water"),
                RecipeStepDTO(order=2, details="Knead dough")
            ]
        )
        
        assert details.servingSize == 4
        assert len(details.recipeIngredients) == 2
        assert len(details.recipeSteps) == 2
        assert details.recipeIngredients[0].name == "Flour"
        assert details.recipeSteps[0].details == "Mix flour and water"
    
    def test_details_validation(self):
        """Test details validation"""
        # All fields are required
        with pytest.raises(ValidationError):
            RecipeDetailsDTO()
        
        with pytest.raises(ValidationError):
            RecipeDetailsDTO(servingSize=4)
        
        with pytest.raises(ValidationError):
            RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[RecipeIngredientDTO(name="Flour")]
            )
        
        # Serving size can be zero or negative in current model (no validation)
        details = RecipeDetailsDTO(
            servingSize=0,
            recipeIngredients=[RecipeIngredientDTO(name="Flour")],
            recipeSteps=[RecipeStepDTO(order=1, details="Test")]
        )
        assert details.servingSize == 0


class TestRecipeData:
    """Test RecipeData model"""
    
    def test_valid_recipe_data(self):
        """Test valid recipe data creation"""
        recipe = RecipeData(
            metadata=RecipeMetadataDTO(
                id=1,
                title="Test Recipe",
                description="A test recipe",
                servingSize=4,
                tags=[RecipeTagDTO(name="Test")]
            ),
            details=RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[RecipeIngredientDTO(name="Test Ingredient")],
                recipeSteps=[RecipeStepDTO(order=1, details="Test step")]
            )
        )
        
        assert recipe.metadata.title == "Test Recipe"
        assert recipe.details.servingSize == 4
        assert len(recipe.details.recipeIngredients) == 1
        assert len(recipe.details.recipeSteps) == 1
    
    def test_recipe_data_validation(self):
        """Test recipe data validation"""
        # Both metadata and details are required
        with pytest.raises(ValidationError):
            RecipeData()
        
        with pytest.raises(ValidationError):
            RecipeData(
                metadata=RecipeMetadataDTO(title="Test")
            )
        
        with pytest.raises(ValidationError):
            RecipeData(
                details=RecipeDetailsDTO(
                    servingSize=4,
                    recipeIngredients=[RecipeIngredientDTO(name="Test")],
                    recipeSteps=[RecipeStepDTO(order=1, details="Test")]
                )
            )


class TestChatRequest:
    """Test ChatRequest model"""
    
    def test_valid_chat_request(self):
        """Test valid chat request creation"""
        request = ChatRequest(message="Hello, how are you?")
        
        assert request.message == "Hello, how are you?"
    
    def test_chat_request_validation(self):
        """Test chat request validation"""
        # Message is required
        with pytest.raises(ValidationError):
            ChatRequest()
        
        # Empty message is allowed in current model
        request = ChatRequest(message="")
        assert request.message == ""


class TestRecipeIndexRequest:
    """Test RecipeIndexRequest model"""
    
    def test_valid_index_request(self):
        """Test valid index request creation"""
        request = RecipeIndexRequest(
            recipe=RecipeData(
                metadata=RecipeMetadataDTO(title="Test Recipe"),
                details=RecipeDetailsDTO(
                    servingSize=4,
                    recipeIngredients=[RecipeIngredientDTO(name="Test")],
                    recipeSteps=[RecipeStepDTO(order=1, details="Test")]
                )
            )
        )
        
        assert request.recipe.metadata.title == "Test Recipe"
    
    def test_index_request_validation(self):
        """Test index request validation"""
        # Recipe is required
        with pytest.raises(ValidationError):
            RecipeIndexRequest()


class TestRecipeDeleteRequest:
    """Test RecipeDeleteRequest model"""
    
    def test_valid_delete_request(self):
        """Test valid delete request creation"""
        request = RecipeDeleteRequest(recipe_id=123)
        
        assert request.recipe_id == 123
    
    def test_delete_request_validation(self):
        """Test delete request validation"""
        # Recipe ID is required
        with pytest.raises(ValidationError):
            RecipeDeleteRequest()


class TestRecipeSuggestionRequest:
    """Test RecipeSuggestionRequest model"""
    
    def test_valid_suggestion_request(self):
        """Test valid suggestion request creation"""
        request = RecipeSuggestionRequest(query="I want something spicy")
        
        assert request.query == "I want something spicy"
    
    def test_suggestion_request_validation(self):
        """Test suggestion request validation"""
        # Query is required
        with pytest.raises(ValidationError):
            RecipeSuggestionRequest()
        
        # Empty query is allowed in current model
        request = RecipeSuggestionRequest(query="")
        assert request.query == ""


class TestChatResponse:
    """Test ChatResponse model"""
    
    def test_valid_chat_response(self):
        """Test valid chat response creation"""
        response = ChatResponse(
            reply="Hello! How can I help you?",
            sources=["source1", "source2"],
            recipe_suggestion={"title": "Test Recipe"}
        )
        
        assert response.reply == "Hello! How can I help you?"
        assert response.sources == ["source1", "source2"]
        assert response.recipe_suggestion["title"] == "Test Recipe"
        assert isinstance(response.timestamp, datetime)
    
    def test_chat_response_without_optional_fields(self):
        """Test chat response creation without optional fields"""
        response = ChatResponse(reply="Simple reply")
        
        assert response.reply == "Simple reply"
        assert response.sources is None
        assert response.recipe_suggestion is None
        assert isinstance(response.timestamp, datetime)
    
    def test_chat_response_validation(self):
        """Test chat response validation"""
        # Reply is required
        with pytest.raises(ValidationError):
            ChatResponse()
        
        # Empty reply is allowed in current model
        response = ChatResponse(reply="")
        assert response.reply == ""


class TestRecipeIndexResponse:
    """Test RecipeIndexResponse model"""
    
    def test_valid_index_response(self):
        """Test valid index response creation"""
        response = RecipeIndexResponse(
            message="Recipe indexed successfully",
            recipe_id="123"
        )
        
        assert response.message == "Recipe indexed successfully"
        assert response.recipe_id == "123"
        assert isinstance(response.indexed_at, datetime)
    
    def test_index_response_validation(self):
        """Test index response validation"""
        # Both fields are required
        with pytest.raises(ValidationError):
            RecipeIndexResponse()
        
        with pytest.raises(ValidationError):
            RecipeIndexResponse(message="Test")
        
        with pytest.raises(ValidationError):
            RecipeIndexResponse(recipe_id="123")


class TestRecipeDeleteResponse:
    """Test RecipeDeleteResponse model"""
    
    def test_valid_delete_response(self):
        """Test valid delete response creation"""
        response = RecipeDeleteResponse(
            message="Recipe deleted successfully",
            recipe_id="123"
        )
        
        assert response.message == "Recipe deleted successfully"
        assert response.recipe_id == "123"
        assert isinstance(response.deleted_at, datetime)
    
    def test_delete_response_validation(self):
        """Test delete response validation"""
        # Both fields are required
        with pytest.raises(ValidationError):
            RecipeDeleteResponse()
        
        with pytest.raises(ValidationError):
            RecipeDeleteResponse(message="Test")
        
        with pytest.raises(ValidationError):
            RecipeDeleteResponse(recipe_id="123")


class TestRecipeSuggestionResponse:
    """Test RecipeSuggestionResponse model"""
    
    def test_valid_suggestion_response(self):
        """Test valid suggestion response creation"""
        response = RecipeSuggestionResponse(
            suggestion="Here's a great recipe for you",
            recipe_data={"title": "Spicy Pasta", "ingredients": ["pasta", "sauce"]}
        )
        
        assert response.suggestion == "Here's a great recipe for you"
        assert response.recipe_data["title"] == "Spicy Pasta"
        assert isinstance(response.timestamp, datetime)
    
    def test_suggestion_response_validation(self):
        """Test suggestion response validation"""
        # Both fields are required
        with pytest.raises(ValidationError):
            RecipeSuggestionResponse()
        
        with pytest.raises(ValidationError):
            RecipeSuggestionResponse(suggestion="Test")
        
        with pytest.raises(ValidationError):
            RecipeSuggestionResponse(recipe_data={"title": "Test"})


class TestHealthResponse:
    """Test HealthResponse model"""
    
    def test_valid_health_response(self):
        """Test valid health response creation"""
        response = HealthResponse(
            status="healthy",
            services={"llm": "healthy", "vector_store": "healthy"}
        )
        
        assert response.status == "healthy"
        assert response.services["llm"] == "healthy"
        assert response.services["vector_store"] == "healthy"
        assert isinstance(response.timestamp, datetime)
    
    def test_health_response_validation(self):
        """Test health response validation"""
        # Both fields are required
        with pytest.raises(ValidationError):
            HealthResponse()
        
        with pytest.raises(ValidationError):
            HealthResponse(status="healthy")
        
        with pytest.raises(ValidationError):
            HealthResponse(services={"llm": "healthy"})


class TestModelSerialization:
    """Test model serialization and deserialization"""
    
    def test_recipe_data_serialization(self):
        """Test RecipeData serialization"""
        recipe = RecipeData(
            metadata=RecipeMetadataDTO(
                id=1,
                title="Test Recipe",
                description="A test recipe",
                servingSize=4,
                tags=[RecipeTagDTO(name="Test")]
            ),
            details=RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[RecipeIngredientDTO(name="Test Ingredient")],
                recipeSteps=[RecipeStepDTO(order=1, details="Test step")]
            )
        )
        
        # Test serialization to dict
        recipe_dict = recipe.model_dump()
        assert recipe_dict["metadata"]["title"] == "Test Recipe"
        assert recipe_dict["details"]["servingSize"] == 4
        
        # Test serialization to JSON
        recipe_json = recipe.model_dump_json()
        assert "Test Recipe" in recipe_json
        
        # Test deserialization from dict
        recipe_from_dict = RecipeData.model_validate(recipe_dict)
        assert recipe_from_dict.metadata.title == "Test Recipe"
    
    def test_chat_response_serialization(self):
        """Test ChatResponse serialization"""
        response = ChatResponse(
            reply="Test reply",
            sources=["source1"],
            recipe_suggestion={"title": "Test Recipe"}
        )
        
        # Test serialization to dict
        response_dict = response.model_dump()
        assert response_dict["reply"] == "Test reply"
        assert response_dict["sources"] == ["source1"]
        assert response_dict["recipe_suggestion"]["title"] == "Test Recipe"
        
        # Test serialization to JSON
        response_json = response.model_dump_json()
        assert "Test reply" in response_json
        
        # Test deserialization from dict
        response_from_dict = ChatResponse.model_validate(response_dict)
        assert response_from_dict.reply == "Test reply"


class TestModelValidation:
    """Test model validation scenarios"""
    
    def test_recipe_with_empty_ingredients(self):
        """Test recipe with empty ingredients list"""
        recipe = RecipeData(
            metadata=RecipeMetadataDTO(title="Empty Recipe"),
            details=RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[],
                recipeSteps=[RecipeStepDTO(order=1, details="Test step")]
            )
        )
        
        assert len(recipe.details.recipeIngredients) == 0
    
    def test_recipe_with_empty_steps(self):
        """Test recipe with empty steps list"""
        recipe = RecipeData(
            metadata=RecipeMetadataDTO(title="Empty Recipe"),
            details=RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[RecipeIngredientDTO(name="Test")],
                recipeSteps=[]
            )
        )
        
        assert len(recipe.details.recipeSteps) == 0
    
    def test_recipe_with_empty_tags(self):
        """Test recipe with empty tags list"""
        recipe = RecipeData(
            metadata=RecipeMetadataDTO(
                title="Empty Recipe",
                tags=[]
            ),
            details=RecipeDetailsDTO(
                servingSize=4,
                recipeIngredients=[RecipeIngredientDTO(name="Test")],
                recipeSteps=[RecipeStepDTO(order=1, details="Test step")]
            )
        )
        
        assert len(recipe.metadata.tags) == 0
    
    def test_recipe_with_large_numbers(self):
        """Test recipe with large numbers"""
        recipe = RecipeData(
            metadata=RecipeMetadataDTO(
                id=999999,
                title="Large Recipe",
                servingSize=100
            ),
            details=RecipeDetailsDTO(
                servingSize=100,
                recipeIngredients=[
                    RecipeIngredientDTO(name="Flour", amount=1000.5)
                ],
                recipeSteps=[RecipeStepDTO(order=999, details="Final step")]
            )
        )
        
        assert recipe.metadata.id == 999999
        assert recipe.details.servingSize == 100
        assert recipe.details.recipeIngredients[0].amount == 1000.5
        assert recipe.details.recipeSteps[0].order == 999 