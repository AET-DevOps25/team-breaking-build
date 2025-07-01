from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    """Application settings"""
    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}
    
    # LLM Configuration
    llm_base_url: str = Field(default="http://localhost:11434", description="LLM API base URL")
    llm_api_key: Optional[str] = Field(default=None, description="LLM API key")
    llm_chat_model: Optional[str] = Field(default=None, description="LLM chat model")
    llm_embedding_model: Optional[str] = Field(default=None, description="LLM embedding model")
    
    # Weaviate Configuration
    weaviate_url: str = Field(default="http://localhost:8080", description="Weaviate server URL")
    weaviate_api_key: Optional[str] = Field(default=None, description="Weaviate API key")
    weaviate_collection_name: str = Field(default="recipes", description="Weaviate collection name")
    weaviate_index_name: Optional[str] = Field(default=None, description="Weaviate index name")
    
    # Application Configuration
    debug: bool = Field(default=False, description="Debug mode")
    app_host: str = Field(default="0.0.0.0", description="Application host")
    app_port: int = Field(default=8000, description="Application port")
    log_level: str = Field(default="INFO", description="Log level")
    
    # Recipe Service Configuration
    recipe_service_url: Optional[str] = Field(default="http://localhost:8081", description="Recipe service URL")

# Global settings instance
settings = Settings() 