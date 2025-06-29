import os
from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """Application settings"""
    
    # LLM Configuration
    LLM_BASE_URL: str = os.getenv("LLM_BASE_URL", "http://localhost:11434")
    LLM_API_KEY: Optional[str] = os.getenv("LLM_API_KEY")
    LLM_CHAT_MODEL: str = os.getenv("LLM_CHAT_MODEL", "llama2")
    LLM_EMBEDDING_MODEL: str = os.getenv("LLM_EMBEDDING_MODEL", "llama2")
    
    # Vector Store Configuration
    WEAVIATE_URL: str = os.getenv("WEAVIATE_URL", "http://localhost:8080")
    WEAVIATE_API_KEY: Optional[str] = os.getenv("WEAVIATE_API_KEY")
    WEAVIATE_INDEX_NAME: str = os.getenv("WEAVIATE_INDEX_NAME", "Recipe")
    
    # Recipe Service Configuration
    RECIPE_SERVICE_URL: str = os.getenv("RECIPE_SERVICE_URL", "http://localhost:8081")
    
    # Application Configuration
    APP_HOST: str = os.getenv("APP_HOST", "0.0.0.0")
    APP_PORT: int = int(os.getenv("APP_PORT", "8000"))
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"
    
    # Logging Configuration
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=True
    )

# Global config instance
config = Settings() 