# GenAI Recipe Service

A Python FastAPI service that provides AI-powered recipe generation, modification, and search capabilities using Large Language Models (LLMs) and Retrieval-Augmented Generation (RAG) with vector databases.

## Overview

The GenAI service combines the power of Large Language Models with a vector database to provide intelligent recipe assistance. It offers conversational recipe interaction, ingredient-based recipe generation, recipe modification suggestions, and semantic recipe search capabilities.

## Key Features

### 🤖 AI-Powered Recipe Generation
- **Intelligent Recipe Creation**: Generate recipes from natural language descriptions
- **Ingredient-Based Generation**: Create recipes using available ingredients
- **Dietary Adaptations**: Modify recipes for specific dietary requirements
- **Creative Variations**: AI-generated recipe variations and improvements

### 🔍 Semantic Recipe Search
- **Vector-Based Search**: Find recipes using semantic similarity
- **Context-Aware Retrieval**: Retrieve relevant recipes based on user queries
- **Recipe Indexing**: Automatic vectorization and storage of recipe content
- **Multi-modal Search**: Search by ingredients, cooking methods, or descriptions

### 📊 Advanced RAG Pipeline
- **Document Chunking**: Intelligent recipe content segmentation
- **Embedding Generation**: Vector representations using state-of-the-art models
- **Similarity Search**: Fast and accurate recipe retrieval
- **Context Enhancement**: Augment LLM responses with relevant recipe data

## Technology Stack

- **Framework**: FastAPI with async/await support
- **LLM Integration**: LangChain with multiple LLM providers
- **Vector Database**: Weaviate for semantic search
- **Embeddings**: HuggingFace sentence-transformers
- **Text Processing**: LangChain text splitters
- **Monitoring**: Prometheus metrics integration
- **Validation**: Pydantic models for request/response validation

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │    │  Recipe Service │    │  GenAI Service  │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │  Chatbot    │─┼────┼→│   Recipe    │─┼────┼→│   Chat      │ │
│ │ Interface   │ │    │ │ Management  │ │    │ │ Endpoint    │ │
│ │(Chat+Search)│ │    │ └─────────────┘ │    │ └─────────────┘ │
│ └─────────────┘ │    │                 │    │ ┌─────────────┐ │
└─────────────────┘    │                 │    │ │ RAG Engine  │ │
                       │                 │    │ └─────────────┘ │
                       │ ┌─────────────┐ │    │ ┌─────────────┐ │
                       │ │ CRUD Ops    │─┼────┼→│ LLM Service │ │
                       │ │ (triggers)  │ │    │ │             │ │
                       │ └─────────────┘ │    │ └─────────────┘ │
┌─────────────────┐    └─────────────────┘    └─────┼───────────┘
│   Vector DB     │                                 │        ▲
│   (Weaviate)    │←────────────────────────────────┘        │
│        ▲        │                           ┌─────────────────┐
└────────┼────────┘                           │   LLM Provider  │
         │                                    └─────────────────┘
┌─────────────────┐                          ┌─────────────────┐
│  Transformers   │                          │   Monitoring    │
│    Service      │                          │ (Prometheus)    │
│ (text2vec-tf)   │                          └─────────────────┘
└─────────────────┘
```

**Service Dependencies:**
- GenAI Service → Weaviate → Transformers Service
- Deployment order: Transformers → Weaviate → GenAI

## Project Structure

```
genai/
├── main.py                 # FastAPI application and endpoints
├── llm.py                  # LLM service and recipe processing
├── rag.py                  # Retrieval-Augmented Generation logic
├── request_models.py       # Pydantic request models
├── response_models.py      # Pydantic response models
├── requirements.txt        # Python dependencies
├── Dockerfile             # Container configuration
├── env.example            # Environment variables template
└── __init__.py            # Package initialization
```

## API Endpoints

### Chat Interface
```http
POST /genai/chat
Content-Type: application/json

{
  "message": "Create a vegetarian pasta recipe with mushrooms"
}
```

### Recipe Indexing
```http
POST /genai/vector/index
Content-Type: application/json

{
  "recipe": {
    "metadata": {...},
    "details": {...}
  }
}
```

### Recipe Deletion
```http
DELETE /genai/vector/delete
Content-Type: application/json

{
  "recipe_id": "123"
}
```

### Recipe Suggestions
```http
POST /genai/vector/suggest
Content-Type: application/json

{
  "query": "healthy breakfast options"
}
```

### Health Check
```http
GET /genai/health
```

## Development Setup

### Prerequisites

- Python 3.11+
- Poetry or pip for dependency management
- **Transformers Service**: Required for Weaviate text vectorization
- **Weaviate instance**: Vector database for semantic search
- LLM API access (OpenAI, OpenWebUI, etc.)

### Installation

```bash
# Clone and navigate to genai directory
cd genai

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### Environment Configuration

Create a `.env` file based on `env.example`:

```bash
# LLM Configuration
LLM_MODEL=llama3.3:latest
LLM_TEMPERATURE=0.7
OPEN_WEBUI_API_KEY=your_api_key_here
LLM_BASE_URL=https://your.base.url

# Weaviate Configuration
WEAVIATE_HOST=weaviate
WEAVIATE_PORT=8080

# Application Configuration
DEBUG=false
LOG_LEVEL=INFO
```

### Running the Service

```bash
# Development server with auto-reload
uvicorn main:app --reload --host 0.0.0.0 --port 8080

# Production server
uvicorn main:app --host 0.0.0.0 --port 8080 --workers 4
```

The service will be available at [http://localhost:8080](http://localhost:8080).

## Dependencies

### Transformers Service

Weaviate requires a running transformers inference service for text vectorization. The GenAI service depends on this infrastructure.

#### Key Configuration
- **Service**: `sentence-transformers-multi-qa-MiniLM-L6-cos-v1`
- **Endpoint**: `http://transformers.recipefy.svc.cluster.local:8080`
- **Model**: Optimized for Q&A and semantic search tasks

#### Local Development Setup
```bash
# Run transformers service with Docker
docker run -p 8080:8080 \
  -e ENABLE_CUDA=0 \
  -e MODEL_NAME=sentence-transformers-multi-qa-MiniLM-L6-cos-v1 \
  cr.weaviate.io/semitechnologies/transformers-inference:sentence-transformers-multi-qa-MiniLM-L6-cos-v1
```

### Vector Database (Weaviate)

Weaviate provides semantic search capabilities and requires the transformers service to be running.

#### Setup and Configuration

```bash
# Run Weaviate with Docker (requires transformers service)
docker run -p 8080:8080 -p 50051:50051 \
  -e ENABLE_MODULES=text2vec-transformers \
  -e TRANSFORMERS_INFERENCE_API=http://transformers:8080 \
  semitechnologies/weaviate:1.24.1
```

#### Recipe Schema

The service automatically creates a Weaviate schema for recipe documents:

```python
{
    "class": "Recipe",
    "properties": [
        {"name": "content", "dataType": ["text"]},
        {"name": "recipe_id", "dataType": ["string"]},
        {"name": "title", "dataType": ["string"]},
        {"name": "description", "dataType": ["text"]},
        {"name": "ingredients", "dataType": ["text"]},
        {"name": "steps", "dataType": ["text"]}
    ]
}
```

## RAG Pipeline

### Document Processing

1. **Recipe Ingestion**: Structured recipe data from Recipe Service
2. **Content Preparation**: Format recipe content for vectorization
3. **Text Chunking**: Split large recipes into manageable chunks
4. **Embedding Generation**: Create vector representations
5. **Vector Storage**: Store in Weaviate with metadata

### Retrieval Process

1. **Query Embedding**: Convert user query to vector
2. **Similarity Search**: Find relevant recipes in vector space
3. **Context Preparation**: Format retrieved content for LLM
4. **Response Generation**: Use LLM with augmented context

## Monitoring and Observability

### Prometheus Metrics

The service exposes metrics at `/metrics`:

- Request counts and latencies
- LLM response times
- Vector search performance
- Error rates and types

### Structured Logging

```python
structured_logger.info(
    "Recipe generation completed",
    extra={
        'duration_ms': 1250,
        'extra_context': {
            'operation': 'recipe_generation',
            'model': 'gpt-4',
            'tokens_used': 150
        }
    }
)
```

### Health Checks

```http
GET /genai/health

{
  "status": "healthy",
  "services": {
    "llm": "connected",
    "weaviate": "connected",
    "embeddings": "loaded"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## Testing

### Running Tests

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=. --cov-report=html

# Run specific test file
pytest test_llm.py -v
```

### Test Structure

```
tests/
├── test_main.py           # API endpoint tests
├── test_llm.py            # LLM service tests
├── test_rag.py            # RAG pipeline tests
└── conftest.py            # Test configuration
```

## Production Deployment

### Docker Build

```bash
# Build image
docker build -t recipefy-genai .

# Run container
docker run -p 8080:8080 --env-file .env recipefy-genai
```

### Environment Variables

Production environment configuration:

```bash
# Performance
WORKERS=4
MAX_REQUESTS=1000
TIMEOUT=30

# Security
DEBUG=false
LOG_LEVEL=INFO

# Resources
MEMORY_LIMIT=4Gi
CPU_LIMIT=2000m
```

### Kubernetes Deployment

The GenAI service requires a specific deployment order due to dependencies.

#### Deployment Order
1. **Deploy Transformers Service** (required by Weaviate)
2. **Deploy Weaviate** (required by GenAI)
3. **Deploy GenAI Service**

#### Manual Deployment

```bash
# 1. Deploy Transformers Service
helm install transformers ./helm/transformers \
  --namespace recipefy \
  --create-namespace

# 2. Deploy Weaviate with transformers dependency
helm install weaviate ./helm/weaviate \
  --namespace recipefy-database \
  --create-namespace

# 3. Deploy GenAI Service
helm install genai ./helm/genai \
  --namespace recipefy \
  --set image.tag="latest"
```

#### Automated CI/CD Deployment

The service uses GitHub Actions for automated deployment:
- **Image Registry**: `ghcr.io/aet-devops25/team-breaking-build/genai`
- **Deployment**: Triggered on pushes to `main` branch
- **Testing**: Includes syntax validation and health checks
- **Monitoring**: Integrated with Grafana annotations

Configuration in `.github/workflows/genai-ci-cd.yml`:
```yaml
env:
  REGISTRY: ghcr.io
  IMAGE_NAME: genai
  HELM_NAMESPACE: recipefy
  CHART_PATH: ./helm/genai
```

## Security

### API Security
- Request validation with Pydantic
- Input sanitization and length limits
- Error handling without information leakage

### LLM Security
- Output filtering and validation
- API key management
- Usage monitoring and limits

## Troubleshooting

### Common Issues

1. **Transformers Service Not Available**
   ```bash
   # Check transformers service status
   curl http://transformers.recipefy.svc.cluster.local:8080/meta
   
   # Local development check
   curl http://localhost:8080/meta
   
   # Verify transformers pod is running
   kubectl get pods -n recipefy -l app=transformers
   ```

2. **Weaviate Connection Failed**
   ```bash
   # Check Weaviate status
   curl http://weaviate.recipefy-database.svc.cluster.local:8080/v1/meta
   
   # Local development check
   curl http://localhost:8080/v1/meta
   
   # Verify environment variables
   echo $WEAVIATE_HOST $WEAVIATE_PORT
   
   # Check if transformers is configured
   curl http://localhost:8080/v1/modules/text2vec-transformers/meta
   ```

3. **LLM API Errors**
   ```bash
   # Test API connectivity
   curl -H "Authorization: Bearer $API_KEY" $LLM_BASE_URL/health
   
   # Check rate limits and quotas
   ```

4. **Vector Embedding Issues**
   ```bash
   # Check if transformers service is responding
   curl -X POST http://localhost:8080/vectors \
     -H "Content-Type: application/json" \
     -d '{"text": "test query"}'
   
   # Clear model cache if needed
   rm -rf ~/.cache/huggingface/transformers/
   
   # Reinstall sentence-transformers
   pip install --force-reinstall sentence-transformers
   ```

5. **Kubernetes Deployment Issues**
   ```bash
   # Check deployment order dependencies
   kubectl get pods -n recipefy | grep transformers
   kubectl get pods -n recipefy-database | grep weaviate
   kubectl get pods -n recipefy | grep genai
   
   # Check service connectivity
   kubectl exec -it <genai-pod> -- curl http://weaviate.recipefy-database.svc.cluster.local:8080/v1/meta
   ```

### Debugging

Enable debug logging:

```bash
export DEBUG=true
export LOG_LEVEL=DEBUG
```

Check service logs:

```bash
# Docker container logs
docker logs genai-container

# Local development logs
tail -f genai.log
```

## Integration with Recipe Service

### Recipe Indexing Flow

1. Recipe Service calls `/genai/vector/index` on recipe creation
2. GenAI processes recipe content and metadata
3. Content is chunked and embedded
4. Vectors stored in Weaviate with searchable metadata

### Recipe Deletion Flow

1. Recipe Service calls `/genai/vector/delete` on recipe deletion
2. GenAI removes all associated vectors from Weaviate
3. Cleanup of any cached embeddings or temporary data

### Chat Integration

1. Frontend sends user message to Recipe Service
2. Recipe Service forwards to GenAI `/genai/chat`
3. GenAI performs RAG retrieval and LLM generation
4. Response includes both text and structured recipe data
5. Frontend displays chat response and recipe cards
