# Recipe Service

A Spring Boot microservice responsible for managing recipe metadata, CRUD operations, and coordination with version control and AI services within the Recipefy platform.

## Overview

The Recipe Service handles the core recipe management functionality, including recipe creation, editing, deletion, and retrieval. It maintains recipe metadata such as titles, descriptions, thumbnails, serving sizes, and tags, while delegating recipe content versioning to the Version Control Service and AI-powered features to the GenAI Service.

## Key Features

### 📝 Recipe Management
- **CRUD Operations**: Create, read, update, and delete recipes
- **Recipe Metadata**: Manage titles, descriptions, thumbnails, and serving information
- **Tag System**: Categorize recipes with customizable tags
- **User Ownership**: Associate recipes with user accounts and enforce ownership

### 🔗 Service Integration
- **Version Control**: Initialize and manage recipe version branches
- **AI Integration**: Index recipes for AI-powered search and suggestions
- **User Authentication**: Validate user permissions via JWT tokens
- **Cross-Service Communication**: REST client communication with dependent services

### 🏷️ Tagging System
- **Tag Management**: Create and manage recipe tags
- **Recipe Categorization**: Associate multiple tags with recipes
- **Tag Retrieval**: List all available tags for filtering and organization

### 👥 User-Centric Features
- **User Recipes**: List recipes owned by specific users
- **Recipe Copying**: Fork recipes from other users with proper attribution
- **Ownership Validation**: Ensure users can only modify their own recipes
- **Public Recipe Discovery**: Browse all public recipes with pagination

## Technology Stack

- **Framework**: Spring Boot 3.4.5 with Spring MVC
- **Language**: Java 21
- **Database**: PostgreSQL with JPA/Hibernate
- **Build Tool**: Gradle
- **Testing**: Spring Boot Test with H2 for testing
- **Documentation**: SpringDoc OpenAPI
- **Monitoring**: Actuator with Prometheus metrics
- **Logging**: Logback with structured logging

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │    │ Recipe Service  │    │ Version Service │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │  Recipe     │─┼────┼→│   Recipe    │─┼────┼→│   Branch    │ │
│ │  Forms      │ │    │ │ Controller  │ │    │ │ Management  │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    │ ┌─────────────┐ │    └─────────────────┘
                       │ │   Recipe    │ │
┌─────────────────┐    │ │  Service    │ │    ┌─────────────────┐
│   PostgreSQL    │◄───┼─┤             │─┼────┼→  GenAI Service │
│   Database      │    │ └─────────────┘ │    │                 │
└─────────────────┘    │ ┌─────────────┐ │    │ ┌─────────────┐ │
                       │ │ Recipe      │ │    │ │   Recipe    │ │
┌─────────────────┐    │ │ Repository  │ │    │ │  Indexing   │ │
│  API Gateway    │◄───┼─┤             │ │    │ └─────────────┘ │
│ (Authentication)│    │ └─────────────┘ │    └─────────────────┘
└─────────────────┘    └─────────────────┘
```

## Project Structure

```
server/recipe/
├── src/
│   ├── main/
│   │   ├── java/com/recipefy/recipe/
│   │   │   ├── RecipeApplication.java          # Main application
│   │   │   ├── controller/                     # REST controllers
│   │   │   │   ├── RecipeController.java       # Recipe CRUD endpoints
│   │   │   │   ├── UserRecipeController.java   # User-specific recipes
│   │   │   │   └── TagController.java          # Tag management
│   │   │   ├── service/                        # Business logic
│   │   │   │   ├── RecipeService.java          # Recipe business logic
│   │   │   │   └── RecipeServiceImpl.java      # Service implementation
│   │   │   ├── repository/                     # Data access layer
│   │   │   │   ├── RecipeRepository.java       # Recipe JPA repository
│   │   │   │   └── TagRepository.java          # Tag JPA repository
│   │   │   ├── client/                         # External service clients
│   │   │   │   ├── VersionClient.java          # Version service client
│   │   │   │   ├── VersionClientImpl.java
│   │   │   │   ├── GenAIClient.java            # GenAI service client
│   │   │   │   └── GenAIClientImpl.java
│   │   │   ├── model/                          # Data models
│   │   │   │   ├── entity/                     # JPA entities
│   │   │   │   ├── dto/                        # Data transfer objects
│   │   │   │   └── request/                    # Request models
│   │   │   ├── mapper/                         # Entity-DTO mappers
│   │   │   ├── config/                         # Configuration classes
│   │   │   ├── exception/                      # Custom exceptions
│   │   │   ├── annotation/                     # Custom annotations
│   │   │   ├── aspect/                         # AOP aspects
│   │   │   └── util/                           # Utility classes
│   │   └── resources/
│   │       ├── application.yml                 # Application configuration
│   │       └── logback-spring.xml             # Logging configuration
│   └── test/                                  # Unit and integration tests
├── build.gradle                               # Build configuration
├── Dockerfile                                 # Container image
└── settings.gradle                            # Gradle settings
```

## Data Model

### Recipe Metadata Entity

```java
@Entity
public class RecipeMetadata extends BaseEntity {
    @Column(updatable = false, nullable = false)
    private UUID userId;
    
    @Column(updatable = false)
    private Long forkedFrom;
    
    private String title;
    private String description;
    private byte[] thumbnail;
    private Integer servingSize;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "recipe_tags",
        joinColumns = @JoinColumn(name = "recipe_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<RecipeTag> tags = new HashSet<>();
}
```

### Recipe Tag Entity

```java
@Entity
public class RecipeTag extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String name;
    
    @ManyToMany(mappedBy = "tags")
    private Set<RecipeMetadata> recipes = new HashSet<>();
}
```

## API Endpoints

### Recipe CRUD Operations

#### Get All Recipes
```http
GET /recipes?page=0&size=20
Authorization: Bearer <token>

Response:
{
  "content": [
    {
      "id": 1,
      "title": "Chocolate Chip Cookies",
      "description": "Classic homemade cookies",
      "servingSize": 24,
      "thumbnail": { "base64String": "..." },
      "tags": [{"id": 1, "name": "dessert"}],
      "userId": "user-uuid",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 5
}
```

#### Get Recipe by ID
```http
GET /recipes/{id}
Authorization: Bearer <token>

Response: Recipe metadata object
```

#### Create Recipe
```http
POST /recipes
Authorization: Bearer <token>
Content-Type: application/json

{
  "metadata": {
    "title": "New Recipe",
    "description": "A delicious new recipe",
    "servingSize": 4,
    "tags": [{"id": 1}]
  },
  "initRequest": {
    "recipeDetails": {
      "servingSize": 4,
      "recipeIngredients": [
        {"name": "flour", "amount": 2, "unit": "cups"}
      ],
      "recipeSteps": [
        {"order": 1, "details": "Mix ingredients"}
      ]
    }
  }
}
```

#### Update Recipe
```http
PUT /recipes/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Updated Recipe Title",
  "description": "Updated description",
  "servingSize": 6,
  "tags": [{"id": 1}, {"id": 2}]
}
```

#### Delete Recipe
```http
DELETE /recipes/{id}
Authorization: Bearer <token>

Response: 204 No Content
```

#### Copy Recipe
```http
POST /recipes/{id}/copy?branchId=123
Authorization: Bearer <token>

Response: New recipe metadata with forkedFrom reference
```

### User-Specific Endpoints

#### Get User's Recipes
```http
GET /users/{userId}/recipes?page=0&size=20
Authorization: Bearer <token>

Response: Paginated list of user's recipes
```

#### Get Recipes by IDs (Batch)
```http
GET /recipes/batch?ids=1,2,3
Authorization: Bearer <token>

Response: List of recipe metadata objects
```

### Tag Management

#### Get All Tags
```http
GET /recipes/tags

Response:
[
  {"id": 1, "name": "dessert"},
  {"id": 2, "name": "vegetarian"},
  {"id": 3, "name": "quick-meals"}
]
```

## Service Integration

### Version Control Integration

The Recipe Service integrates with the Version Control Service for recipe versioning:

```java
@Service
public class RecipeServiceImpl implements RecipeService {
    
    @Override
    @Transactional
    public RecipeMetadataDTO createRecipe(CreateRecipeRequest request, UUID userId) {
        // 1. Save recipe metadata
        RecipeMetadata saved = recipeRepository.save(recipe);
        
        // 2. Initialize version control
        BranchDTO branch = versionClient.initRecipe(saved.getId(), request.getInitRequest(), userId);
        
        // 3. Index for AI search
        genAIClient.indexRecipe(RecipeMetadataDTOMapper.toDTO(saved), request.getInitRequest().getRecipeDetails());
        
        return RecipeMetadataDTOMapper.toDTO(saved);
    }
}
```

### GenAI Integration

Recipes are automatically indexed for AI-powered search:

```java
@Override
public void deleteRecipe(Long recipeId, UUID userId) {
    validateRecipeOwnership(recipeId, userId);
    
    // Remove from AI index
    genAIClient.deleteRecipe(recipeId.toString());
    
    // Delete from database
    recipeRepository.deleteById(recipeId);
}
```

## Configuration

### Application Properties

```yaml
spring:
  application:
    name: recipe
  datasource:
    url: jdbc:postgresql://${RECIPE_POSTGRES_URL_HOST}:${RECIPE_POSTGRES_URL_PORT}/${RECIPE_POSTGRES_DB_NAME}
    username: ${RECIPE_POSTGRES_USER}
    password: ${RECIPE_POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

vcs:
  service:
    url: ${VERSION_SERVICE_HOST}:${VERSION_SERVICE_PORT}

genai:
  service:
    url: ${GENAI_SERVICE_HOST}:${GENAI_SERVICE_PORT}
```

### Environment Variables

```bash
# Database Configuration
RECIPE_POSTGRES_URL_HOST=localhost
RECIPE_POSTGRES_URL_PORT=5432
RECIPE_POSTGRES_DB_NAME=recipefy_recipes
RECIPE_POSTGRES_USER=recipe_user
RECIPE_POSTGRES_PASSWORD=recipe_password

# Service URLs
VERSION_SERVICE_HOST=http://version-service
VERSION_SERVICE_PORT=8080
GENAI_SERVICE_HOST=http://genai-service
GENAI_SERVICE_PORT=8080
```

## Development Setup

### Prerequisites

- Java 21 or higher
- Gradle 8.x
- PostgreSQL database
- Version Control Service running
- GenAI Service running (optional for basic functionality)

### Running Locally

```bash
# Start PostgreSQL (if using Docker)
docker run --name postgres-recipe \
  -e POSTGRES_DB=recipefy_recipes \
  -e POSTGRES_USER=recipe_user \
  -e POSTGRES_PASSWORD=recipe_password \
  -p 5432:5432 -d postgres:17

# Run the service
./gradlew bootRun
```

The service will be available at [http://localhost:8080](http://localhost:8080).

### Database Migration

The service uses Hibernate DDL auto-update for development. For production, consider using Flyway or Liquibase:

```sql
-- Example migration script
CREATE TABLE recipe_metadata (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    forked_from BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail BYTEA,
    serving_size INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE recipe_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE recipe_tags (
    recipe_id BIGINT REFERENCES recipe_metadata(id),
    tag_id BIGINT REFERENCES recipe_tag(id),
    PRIMARY KEY (recipe_id, tag_id)
);
```

## Security and Authorization

### User Authentication

The service relies on the API Gateway for authentication. User information is extracted from JWT tokens:

```java
public class HeaderUtil {
    public static UUID extractRequiredUserIdFromHeader() {
        HttpServletRequest request = getCurrentHttpRequest();
        String userIdHeader = request.getHeader("X-User-Id");
        
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new UnauthorizedException("User ID header is missing");
        }
        
        return UUID.fromString(userIdHeader);
    }
}
```

### Ownership Validation

Users can only modify recipes they own:

```java
private RecipeMetadata validateRecipeOwnership(Long recipeId, UUID userId) {
    RecipeMetadata recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));
    
    if (!recipe.getUserId().equals(userId)) {
        throw new UnauthorizedException("You don't have permission to access this recipe");
    }
    
    return recipe;
}
```

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {
    
    @Mock
    private RecipeRepository recipeRepository;
    
    @Mock
    private VersionClient versionClient;
    
    @InjectMocks
    private RecipeServiceImpl recipeService;
    
    @Test
    void createRecipe_ShouldCreateAndReturnRecipe() {
        // Given
        CreateRecipeRequest request = new CreateRecipeRequest(testRecipeDTO, new InitRecipeRequest(null));
        when(recipeRepository.save(any(RecipeMetadata.class))).thenReturn(testRecipe);
        when(versionClient.initRecipe(anyLong(), any(), any())).thenReturn(testBranchDTO);
        
        // When
        RecipeMetadataDTO result = recipeService.createRecipe(request, userId);
        
        // Then
        assertNotNull(result);
        assertEquals(testRecipe.getTitle(), result.getTitle());
        verify(recipeRepository).save(any(RecipeMetadata.class));
        verify(versionClient).initRecipe(anyLong(), any(), any());
    }
}
```

### Integration Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test class
./gradlew test --tests "*RecipeServiceImplTest"
```

### Test Configuration

Uses H2 in-memory database for testing:

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Build and Deployment

### Gradle Build

```bash
# Build the application
./gradlew build

# Create JAR file
./gradlew bootJar

# Run code quality checks
./gradlew check
```

### Docker Build

```bash
# Build Docker image
docker build -t recipefy-recipe-service .

# Run container
docker run -p 8080:8080 \
  -e RECIPE_POSTGRES_URL_HOST=postgres \
  -e VERSION_SERVICE_HOST=http://version-service \
  recipefy-recipe-service
```

### Multi-stage Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim-bullseye AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/build/libs/recipe.jar /app/recipe.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "recipe.jar"]
```

## Monitoring and Observability

### Health Checks

```bash
# Service health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db
```

### Prometheus Metrics

Available at `/actuator/prometheus`:

- HTTP request metrics (`http_server_requests`)
- Database connection metrics (`hikaricp_*`)
- JVM metrics (`jvm_*`)
- Custom business metrics (recipe counts, operations)

## Performance Considerations

### Pagination

All list endpoints support pagination:

```java
public Page<RecipeMetadataDTO> getAllRecipes(Pageable pageable) {
    return recipeRepository.findAll(pageable)
            .map(RecipeMetadataDTOMapper::toDTO);
}
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   ```bash
   # Check PostgreSQL status
   docker logs postgres-recipe
   
   # Test connection
   psql -h localhost -U recipe_user -d recipefy_recipes
   ```

2. **Version Service Communication Failed**
   ```bash
   # Check service connectivity
   curl http://version-service:8080/actuator/health
   
   # Verify environment variables
   echo $VERSION_SERVICE_HOST
   ```

3. **Recipe Creation Failed**
   ```bash
   # Check logs for validation errors
   docker logs recipe-service
   
   # Verify user authentication
   curl -H "Authorization: Bearer <token>" http://localhost:8080/recipes
   ```

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.recipefy.recipe: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### Health Diagnostics

```bash
# Check all health indicators
curl http://localhost:8080/actuator/health

# Verify database connectivity
curl http://localhost:8080/actuator/health/db

# Check external service connectivity
curl http://localhost:8080/actuator/health/version-service
```
