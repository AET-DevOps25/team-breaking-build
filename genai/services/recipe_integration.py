import httpx
import asyncio
from typing import List, Dict, Any, Optional
from loguru import logger
from datetime import datetime

from config import config
from models.schemas import (
    RecipeMetadataDTO,
    RecipeDetailsDTO,
    RecipeIngredientDTO,
    RecipeStepDTO,
    RecipeTagDTO,
    InitRecipeRequest
)

class RecipeServiceIntegration:
    """Integration with the recipe microservice"""
    
    def __init__(self):
        self.base_url = config.RECIPE_SERVICE_URL
        self.client = httpx.AsyncClient(timeout=30.0)
    
    async def get_recipe(self, recipe_id: int) -> Optional[RecipeMetadataDTO]:
        """Fetch a single recipe by ID"""
        try:
            response = await self.client.get(f"{self.base_url}/recipes/{recipe_id}")
            if response.status_code == 200:
                data = response.json()
                return RecipeMetadataDTO(**data)
            else:
                logger.warning(f"Failed to fetch recipe {recipe_id}: {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Error fetching recipe {recipe_id}: {e}")
            return None
    
    async def get_all_recipes(self, page: int = 0, size: int = 100) -> List[RecipeMetadataDTO]:
        """Fetch all recipes with pagination"""
        try:
            response = await self.client.get(
                f"{self.base_url}/recipes",
                params={"page": page, "size": size}
            )
            if response.status_code == 200:
                data = response.json()
                content = data.get("content", [])
                return [RecipeMetadataDTO(**recipe) for recipe in content]
            else:
                logger.warning(f"Failed to fetch recipes: {response.status_code}")
                return []
        except Exception as e:
            logger.error(f"Error fetching recipes: {e}")
            return []
    
    async def create_recipe(self, metadata: RecipeMetadataDTO, details: RecipeDetailsDTO) -> Optional[RecipeMetadataDTO]:
        """Create a new recipe"""
        try:
            # Create the recipe using the recipe service API
            init_request = InitRecipeRequest(
                userId=metadata.userId,
                recipeDetails=details
            )
            
            # The recipe service expects both metadata and init request
            response = await self.client.post(
                f"{self.base_url}/recipes",
                json={
                    "metadata": metadata.model_dump(),
                    "initRequest": init_request.model_dump()
                }
            )
            
            if response.status_code == 200:
                data = response.json()
                return RecipeMetadataDTO(**data)
            else:
                logger.warning(f"Failed to create recipe: {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Error creating recipe: {e}")
            return None
    
    async def update_recipe(self, recipe_id: int, metadata: RecipeMetadataDTO) -> Optional[RecipeMetadataDTO]:
        """Update an existing recipe"""
        try:
            response = await self.client.put(
                f"{self.base_url}/recipes/{recipe_id}",
                json=metadata.model_dump()
            )
            
            if response.status_code == 200:
                data = response.json()
                return RecipeMetadataDTO(**data)
            else:
                logger.warning(f"Failed to update recipe {recipe_id}: {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Error updating recipe {recipe_id}: {e}")
            return None
    
    async def update_recipe_tags(self, recipe_id: int, tags: List[RecipeTagDTO]) -> Optional[RecipeMetadataDTO]:
        """Update recipe tags"""
        try:
            response = await self.client.put(
                f"{self.base_url}/recipes/{recipe_id}/tags",
                json=[tag.model_dump() for tag in tags]
            )
            
            if response.status_code == 200:
                data = response.json()
                return RecipeMetadataDTO(**data)
            else:
                logger.warning(f"Failed to update recipe tags {recipe_id}: {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Error updating recipe tags {recipe_id}: {e}")
            return None
    
    async def copy_recipe(self, recipe_id: int, user_id: int, branch_id: int) -> Optional[RecipeMetadataDTO]:
        """Copy a recipe for a user"""
        try:
            response = await self.client.post(
                f"{self.base_url}/recipes/{recipe_id}/copy",
                params={"userId": user_id, "branchId": branch_id}
            )
            
            if response.status_code == 200:
                data = response.json()
                return RecipeMetadataDTO(**data)
            else:
                logger.warning(f"Failed to copy recipe {recipe_id}: {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Error copying recipe {recipe_id}: {e}")
            return None
    
    async def delete_recipe(self, recipe_id: int) -> bool:
        """Delete a recipe"""
        try:
            response = await self.client.delete(f"{self.base_url}/recipes/{recipe_id}")
            return response.status_code == 204
        except Exception as e:
            logger.error(f"Error deleting recipe {recipe_id}: {e}")
            return False
    
    async def sync_all_recipes(self, batch_size: int = 50) -> List[RecipeMetadataDTO]:
        """Sync all recipes from the recipe service"""
        try:
            all_recipes = []
            page = 0
            
            while True:
                recipes = await self.get_all_recipes(page=page, size=batch_size)
                if not recipes:
                    break
                
                all_recipes.extend(recipes)
                page += 1
                
                # Add small delay to prevent overwhelming the service
                await asyncio.sleep(0.1)
            
            logger.info(f"Synced {len(all_recipes)} recipes from recipe service")
            return all_recipes
        except Exception as e:
            logger.error(f"Error syncing recipes: {e}")
            return []
    
    async def close(self):
        """Close the HTTP client"""
        await self.client.aclose()

# Utility functions for data transformation
def transform_recipe_for_indexing(recipe_metadata: RecipeMetadataDTO, recipe_details: Optional[RecipeDetailsDTO] = None) -> Dict[str, Any]:
    """Transform recipe data from recipe service format to indexing format"""
    try:
        return {
            "id": recipe_metadata.id,
            "title": recipe_metadata.title,
            "description": recipe_metadata.description,
            "ingredients": recipe_details.recipeIngredients if recipe_details else [],
            "steps": recipe_details.recipeSteps if recipe_details else [],
            "tags": [tag.name for tag in recipe_metadata.tags],
            "serving_size": recipe_details.servingSize if recipe_details else recipe_metadata.servingSize,
            "user_id": recipe_metadata.userId,
            "created_at": recipe_metadata.createdAt,
            "updated_at": recipe_metadata.updatedAt
        }
    except Exception as e:
        logger.error(f"Error transforming recipe data: {e}")
        return {}

def create_recipe_document(recipe_data: Dict[str, Any]) -> str:
    """Create a document string for vector indexing"""
    try:
        ingredients_text = ", ".join([
            f"{ing.get('name', '')} {ing.get('amount', '')} {ing.get('unit', '')}".strip()
            for ing in recipe_data.get("ingredients", [])
        ])
        
        steps_text = " ".join([
            f"{step.get('order', '')}. {step.get('details', '')}"
            for step in recipe_data.get("steps", [])
        ])
        
        tags_text = ", ".join(recipe_data.get("tags", []))
        
        return f"""
        Title: {recipe_data.get('title', '')}
        Description: {recipe_data.get('description', '')}
        Ingredients: {ingredients_text}
        Steps: {steps_text}
        Tags: {tags_text}
        Serving Size: {recipe_data.get('serving_size', 'Not specified')}
        """
    except Exception as e:
        logger.error(f"Error creating recipe document: {e}")
        return ""

def parse_llm_recipe_response(recipe_text: str) -> tuple[RecipeMetadataDTO, RecipeDetailsDTO]:
    """Parse LLM recipe response into proper DTOs"""
    try:
        lines = recipe_text.split('\n')
        
        # Initialize recipe components
        title = "Custom Recipe"
        description = ""
        ingredients = []
        steps = []
        serving_size = 4
        tags = []
        
        current_section = None
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
                
            if "title:" in line.lower() or "name:" in line.lower():
                title = line.split(":", 1)[1].strip()
            elif "description:" in line.lower():
                description = line.split(":", 1)[1].strip()
            elif "serving" in line.lower() and "size" in line.lower():
                try:
                    serving_size = int(line.split(":", 1)[1].strip())
                except:
                    pass
            elif "ingredients:" in line.lower():
                current_section = "ingredients"
            elif "instructions:" in line.lower() or "steps:" in line.lower():
                current_section = "instructions"
            elif current_section == "ingredients" and line.startswith("-"):
                ingredient_text = line[1:].strip()
                # Parse ingredient: "2 cups flour" -> name: "flour", amount: 2, unit: "cups"
                parts = ingredient_text.split()
                if len(parts) >= 2:
                    try:
                        amount = float(parts[0])
                        unit = parts[1] if len(parts) > 2 else ""
                        name = " ".join(parts[2:]) if len(parts) > 2 else parts[1]
                    except ValueError:
                        amount = None
                        unit = ""
                        name = ingredient_text
                else:
                    amount = None
                    unit = ""
                    name = ingredient_text
                
                ingredients.append(RecipeIngredientDTO(
                    name=name,
                    unit=unit,
                    amount=amount
                ))
            elif current_section == "instructions" and (line.startswith("-") or line[0].isdigit()):
                step_text = line.lstrip("- ").lstrip("0123456789. ")
                steps.append(RecipeStepDTO(
                    order=len(steps) + 1,
                    details=step_text
                ))
        
        # Create DTOs
        metadata = RecipeMetadataDTO(
            title=title,
            description=description,
            servingSize=serving_size,
            userId=1,  # Default user ID
            tags=[RecipeTagDTO(name=tag) for tag in tags]
        )
        
        details = RecipeDetailsDTO(
            servingSize=serving_size,
            recipeIngredients=ingredients,
            recipeSteps=steps
        )
        
        return metadata, details
        
    except Exception as e:
        logger.error(f"Error parsing LLM recipe response: {e}")
        # Return default recipe
        metadata = RecipeMetadataDTO(
            title="Custom Recipe",
            description=recipe_text[:200] + "...",
            servingSize=4,
            userId=1,
            tags=[]
        )
        
        details = RecipeDetailsDTO(
            servingSize=4,
            recipeIngredients=[],
            recipeSteps=[]
        )
        
        return metadata, details 