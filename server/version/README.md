# Version Control Service

A Spring Boot microservice that provides Git-like version control functionality for recipes, enabling branching, committing, history tracking, and change management within the Recipefy platform.

## Overview

The Version Control Service implements a comprehensive versioning system for recipe content, allowing users to create branches, commit changes, view history, and track recipe evolution over time. It uses PostgreSQL for metadata and MongoDB for recipe content storage, providing both relational and document-based data management.

## Key Features

### 🌿 Git-like Branching
- **Branch Management**: Create, manage, and switch between recipe branches
- **Main Branch**: Default branch initialization for new recipes
- **Feature Branches**: Create branches for experimental recipe modifications
- **Branch Isolation**: Independent development of recipe variations

### 📝 Commit System
- **Recipe Commits**: Save recipe changes with descriptive commit messages
- **Commit History**: Chronological tracking of all recipe modifications
- **Parent-Child Relationships**: Maintain commit lineage and history
- **Commit Metadata**: Store author, timestamp, and change descriptions

### 📊 Change Tracking
- **Diff Generation**: Compare recipe versions and show differences
- **Change Visualization**: Detailed view of what changed between commits
- **JSON Patch**: Structured representation of recipe modifications
- **History Analysis**: Track recipe evolution and improvement patterns

### 🔄 Recipe Lifecycle
- **Recipe Initialization**: Set up version control for new recipes
- **Recipe Copying**: Fork recipes with preserved version history
- **Content Storage**: Efficient storage of recipe content in MongoDB
- **Metadata Management**: PostgreSQL-based metadata for fast queries

## Technology Stack

- **Framework**: Spring Boot 3.4.5 with Spring MVC
- **Language**: Java 21
- **Database**: PostgreSQL (metadata) + MongoDB (content)
- **Build Tool**: Gradle
- **Testing**: Spring Boot Test with Testcontainers
- **Documentation**: SpringDoc OpenAPI
- **JSON Processing**: Jackson with JSON Patch support
- **Monitoring**: Actuator with Prometheus metrics

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Recipe Service │    │ Version Control │    │   Client App    │
│                 │    │    Service      │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Recipe    │─┼────┼→│   Version   │◄┼────┼─│   Recipe    │ │
│ │   CRUD      │ │    │ │ Controller  │ │    │ │  History    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    │ ┌─────────────┐ │    └─────────────────┘
                       │ │ Branch      │ │
┌─────────────────┐    │ │ Service     │ │    ┌─────────────────┐
│   PostgreSQL    │◄───┼─┤             │ │    │     MongoDB     │
│  (Metadata)     │    │ └─────────────┘ │    │   (Content)     │
│                 │    │ ┌─────────────┐ │    │                 │
│ • Branches      │    │ │ Commit      │ │    │ • Recipe        │
│ • Commits       │    │ │ Service     │─┼────┼→│ Snapshots     │
│ • Relationships │    │ └─────────────┘ │    │ • Details       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Project Structure

```
server/version/
├── src/
│   ├── main/
│   │   ├── java/com/recipefy/version/
│   │   │   ├── VersionApplication.java          # Main application
│   │   │   ├── controller/                      # REST controllers
│   │   │   │   └── VersionController.java       # Version control endpoints
│   │   │   ├── service/                         # Business logic
│   │   │   │   ├── VersionControlService.java   # Main orchestration
│   │   │   │   ├── BranchService.java           # Branch management
│   │   │   │   ├── CommitService.java           # Commit operations
│   │   │   │   └── RecipeSnapshotService.java   # Content management
│   │   │   ├── repository/                      # Data access layer
│   │   │   │   ├── postgres/                    # PostgreSQL repositories
│   │   │   │   │   ├── BranchRepository.java
│   │   │   │   │   └── CommitRepository.java
│   │   │   │   └── mongo/                       # MongoDB repositories
│   │   │   │       └── RecipeSnapshotRepository.java
│   │   │   ├── model/                           # Data models
│   │   │   │   ├── postgres/                    # JPA entities
│   │   │   │   │   ├── Branch.java
│   │   │   │   │   └── Commit.java
│   │   │   │   ├── mongo/                       # MongoDB documents
│   │   │   │   │   ├── RecipeSnapshot.java
│   │   │   │   │   └── RecipeDetails.java
│   │   │   │   ├── dto/                         # Data transfer objects
│   │   │   │   ├── request/                     # Request models
│   │   │   │   └── response/                    # Response models
│   │   │   ├── converter/                       # Model converters
│   │   │   ├── util/                           # Utility classes
│   │   │   │   └── DiffUtil.java               # JSON diff utilities
│   │   │   ├── config/                         # Configuration classes
│   │   │   ├── exception/                      # Custom exceptions
│   │   │   └── constants/                      # Application constants
│   │   └── resources/
│   │       ├── application.yml                 # Application configuration
│   │       └── logback-spring.xml             # Logging configuration
│   └── test/                                  # Unit and integration tests
├── build.gradle                               # Build configuration
├── Dockerfile                                 # Container image
└── settings.gradle                            # Gradle settings
```

## Data Model

### PostgreSQL Entities

#### Branch Entity
```java
@Entity
public class Branch extends BaseEntity {
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Long recipeId;
    
    @ManyToOne
    @JoinColumn(name = "head_commit_id")
    private Commit headCommit;
    
    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Commit> commits = new HashSet<>();
}
```

#### Commit Entity
```java
@Entity
public class Commit extends BaseEntity {
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private String message;
    
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Commit parent;
    
    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;
}
```

### MongoDB Documents

#### Recipe Snapshot
```java
@Document(collection = "recipe_snapshots")
public class RecipeSnapshot {
    @Id
    private String id; // Corresponds to commit ID
    
    private Long recipeId;
    private RecipeDetails details;
    private LocalDateTime createdAt;
}
```

#### Recipe Details
```java
public class RecipeDetails {
    private int servingSize;
    private List<RecipeIngredient> recipeIngredients;
    private List<RecipeStep> recipeSteps;
    private List<RecipeImage> images;
}
```

## API Endpoints

### Recipe Initialization

#### Initialize Recipe
```http
POST /vcs/recipes/{recipeId}/init
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipeDetails": {
    "servingSize": 4,
    "recipeIngredients": [
      {"name": "flour", "amount": 2, "unit": "cups"}
    ],
    "recipeSteps": [
      {"order": 1, "details": "Mix ingredients thoroughly"}
    ]
  }
}

Response:
{
  "id": 1,
  "name": "main",
  "recipeId": 123,
  "headCommitId": 1,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### Branch Management

#### Get Recipe Branches
```http
GET /vcs/recipes/{recipeId}/branches

Response:
[
  {
    "id": 1,
    "name": "main",
    "recipeId": 123,
    "headCommitId": 1,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  {
    "id": 2,
    "name": "feature-spicy-version",
    "recipeId": 123,
    "headCommitId": 3,
    "createdAt": "2024-01-02T00:00:00Z"
  }
]
```

#### Create Branch
```http
POST /vcs/recipes/{recipeId}/branches
Authorization: Bearer <token>
Content-Type: application/json

{
  "branchName": "feature-low-sodium",
  "sourceBranchId": 1
}

Response: Branch object
```

### Commit Operations

#### Commit to Branch
```http
POST /vcs/branches/{branchId}/commit
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "Reduce salt and add herbs for flavor",
  "recipeDetails": {
    "servingSize": 4,
    "recipeIngredients": [
      {"name": "flour", "amount": 2, "unit": "cups"},
      {"name": "herbs", "amount": 1, "unit": "tablespoon"}
    ],
    "recipeSteps": [
      {"order": 1, "details": "Mix flour and herbs"},
      {"order": 2, "details": "Add remaining ingredients"}
    ]
  }
}

Response:
{
  "id": 4,
  "userId": "user-uuid",
  "message": "Reduce salt and add herbs for flavor",
  "parentId": 3,
  "createdAt": "2024-01-03T00:00:00Z"
}
```

#### Get Branch History
```http
GET /vcs/branches/{branchId}/history

Response:
[
  {
    "id": 4,
    "userId": "user-uuid",
    "message": "Reduce salt and add herbs for flavor",
    "parentId": 3,
    "createdAt": "2024-01-03T00:00:00Z"
  },
  {
    "id": 3,
    "userId": "user-uuid", 
    "message": "Add spicy variation",
    "parentId": 1,
    "createdAt": "2024-01-02T00:00:00Z"
  }
]
```

### Commit Details and Changes

#### Get Commit Details
```http
GET /vcs/commits/{commitId}

Response:
{
  "commit": {
    "id": 4,
    "userId": "user-uuid",
    "message": "Reduce salt and add herbs for flavor",
    "parentId": 3,
    "createdAt": "2024-01-03T00:00:00Z"
  },
  "recipeDetails": {
    "servingSize": 4,
    "recipeIngredients": [...],
    "recipeSteps": [...]
  }
}
```

#### Get Commit Changes
```http
GET /vcs/commits/{commitId}/changes

Response:
{
  "oldRecipe": { /* Previous version */ },
  "newRecipe": { /* Current version */ },
  "changes": {
    "op": "replace",
    "path": "/recipeIngredients/1/amount",
    "value": "1 tablespoon"
  },
  "isFirstCommit": false
}
```

### Recipe Copying

#### Copy Recipe Branch
```http
POST /vcs/branches/{branchId}/copy
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipeId": 456
}

Response: New branch object for copied recipe
```

## Service Implementation

### Version Control Service

The main orchestration service that coordinates between branch, commit, and snapshot services:

```java
@Service
@RequiredArgsConstructor
public class VersionControlService {
    
    @Transactional
    public BranchDTO initRecipe(Long recipeId, InitRecipeRequest request, UUID userId) {
        // 1. Validate branch doesn't exist
        branchService.checkIfBranchCreated(recipeId);
        
        // 2. Create initial commit
        Commit commit = commitService.createInitialCommit(userId);
        
        // 3. Store recipe content
        RecipeSnapshot snapshot = RecipeSnapshotConverter.toDocument(
            commit.getId(), recipeId, request.getRecipeDetails()
        );
        recipeSnapshotService.createRecipeSnapshot(snapshot);
        
        // 4. Create main branch
        Branch branch = branchService.createMainBranch(recipeId, commit);
        
        return BranchDTOConverter.toDTO(branch);
    }
}
```

### Diff Utility

Generates JSON patches for change tracking:

```java
public class DiffUtil {
    public static JsonNode compareRecipes(RecipeDetails oldRecipe, RecipeDetails newRecipe) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode oldNode = mapper.valueToTree(oldRecipe);
            JsonNode newNode = mapper.valueToTree(newRecipe);
            
            return JsonDiff.asJson(oldNode, newNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate recipe diff", e);
        }
    }
}
```

## Configuration

### Application Properties

```yaml
spring:
  application:
    name: version
  datasource:
    url: jdbc:postgresql://${VERSION_POSTGRES_URL_HOST}:${VERSION_POSTGRES_URL_PORT}/${VERSION_POSTGRES_DB_NAME}
    username: ${VERSION_POSTGRES_USER}
    password: ${VERSION_POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
  data:
    mongodb:
      uri: mongodb://${VERSION_MONGO_USER}:${VERSION_MONGO_PASSWORD}@${VERSION_MONGO_URL_HOSTS}/${VERSION_MONGO_DB_NAME}?replicaSet=${VERSION_MONGO_REPLICA_SET}&authSource=admin
```

### Environment Variables

```bash
# PostgreSQL Configuration (Metadata)
VERSION_POSTGRES_URL_HOST=localhost
VERSION_POSTGRES_URL_PORT=5432
VERSION_POSTGRES_DB_NAME=recipefy_version
VERSION_POSTGRES_USER=version_user
VERSION_POSTGRES_PASSWORD=version_password

# MongoDB Configuration (Content)
VERSION_MONGO_USER=version_user
VERSION_MONGO_PASSWORD=version_password
VERSION_MONGO_URL_HOSTS=localhost:27017
VERSION_MONGO_DB_NAME=recipefy_content
VERSION_MONGO_REPLICA_SET=rs0
```

## Development Setup

### Prerequisites

- Java 21 or higher
- Gradle 8.x
- PostgreSQL database
- MongoDB database
- Docker (for easy database setup)

### Running Locally

```bash
# Start databases with Docker Compose
docker-compose up -d postgres mongo

# Or start individual containers
docker run --name postgres-version \
  -e POSTGRES_DB=recipefy_version \
  -e POSTGRES_USER=version_user \
  -e POSTGRES_PASSWORD=version_password \
  -p 5432:5432 -d postgres:17

docker run --name mongo-version \
  -e MONGO_INITDB_ROOT_USERNAME=version_user \
  -e MONGO_INITDB_ROOT_PASSWORD=version_password \
  -p 27017:27017 -d mongo:6.0

# Run the service
./gradlew bootRun
```

The service will be available at [http://localhost:8080](http://localhost:8080).

### Database Setup

#### PostgreSQL Schema
```sql
-- Branches table
CREATE TABLE branch (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    recipe_id BIGINT NOT NULL,
    head_commit_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Commits table
CREATE TABLE commit (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    message TEXT NOT NULL,
    parent_id BIGINT REFERENCES commit(id),
    branch_id BIGINT REFERENCES branch(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Add foreign key constraint
ALTER TABLE branch ADD CONSTRAINT fk_branch_head_commit 
FOREIGN KEY (head_commit_id) REFERENCES commit(id);
```

#### MongoDB Collections
```javascript
// Create recipe_snapshots collection with indexes
db.recipe_snapshots.createIndex({ "recipeId": 1 })
db.recipe_snapshots.createIndex({ "createdAt": -1 })
db.recipe_snapshots.createIndex({ "id": 1 }, { unique: true })
```

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class VersionControlServiceTest {
    
    @Mock
    private BranchService branchService;
    
    @Mock
    private CommitService commitService;
    
    @Mock
    private RecipeSnapshotService recipeSnapshotService;
    
    @InjectMocks
    private VersionControlService versionControlService;
    
    @Test
    void initRecipe_ShouldInitializeRecipeSuccessfully() {
        // Given
        Long recipeId = 1L;
        InitRecipeRequest request = new InitRecipeRequest(sampleRecipeDetails);
        UUID userId = UUID.randomUUID();
        
        doNothing().when(branchService).checkIfBranchCreated(recipeId);
        when(commitService.createInitialCommit(userId)).thenReturn(sampleCommit);
        when(branchService.createMainBranch(recipeId, sampleCommit)).thenReturn(sampleBranch);
        
        // When
        BranchDTO result = versionControlService.initRecipe(recipeId, request, userId);
        
        // Then
        assertNotNull(result);
        verify(branchService).checkIfBranchCreated(recipeId);
        verify(commitService).createInitialCommit(userId);
        verify(recipeSnapshotService).createRecipeSnapshot(any());
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class VersionControlIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:6.0");
    
    @Test
    void fullRecipeVersionControlFlow_ShouldWorkEndToEnd() throws Exception {
        // Test complete workflow from recipe initialization to commit history
        mockMvc.perform(post("/vcs/recipes/1/init")
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("main"));
        
        // Continue with branch creation, commits, etc.
    }
}
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run with Testcontainers (integration tests)
./gradlew integrationTest

# Run with coverage
./gradlew test jacocoTestReport
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
docker build -t recipefy-version-service .

# Run container
docker run -p 8080:8080 \
  -e VERSION_POSTGRES_URL_HOST=postgres \
  -e VERSION_MONGO_URL_HOSTS=mongo:27017 \
  recipefy-version-service
```

### Multi-stage Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim-bullseye AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/build/libs/version.jar /app/version.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "version.jar"]
```

## Monitoring and Observability

### Health Checks

```bash
# Service health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/mongo
```

### Prometheus Metrics

Available at `/actuator/prometheus`:

- HTTP request metrics (`http_server_requests`)
- Database connection metrics (PostgreSQL and MongoDB)
- JVM metrics (`jvm_*`)
- Custom business metrics (commits, branches, operations)

### Custom Metrics

```java
@Component
public class VersionMetrics {
    
    private final Counter commitCounter = Counter.builder("version_commits_total")
            .description("Total number of commits")
            .register(Metrics.globalRegistry);
    
    private final Counter branchCounter = Counter.builder("version_branches_total")
            .description("Total number of branches created")
            .register(Metrics.globalRegistry);
    
    public void incrementCommitCounter() {
        commitCounter.increment();
    }
}
```

## Troubleshooting

### Common Issues

1. **PostgreSQL Connection Failed**
   ```bash
   # Check PostgreSQL status
   docker logs postgres-version
   
   # Test connection
   psql -h localhost -U version_user -d recipefy_version
   ```

2. **MongoDB Connection Failed**
   ```bash
   # Check MongoDB status
   docker logs mongo-version
   
   # Test connection
   mongosh "mongodb://version_user:version_password@localhost:27017/recipefy_content"
   ```

3. **Branch Creation Failed**
   ```bash
   # Check for existing branches
   curl http://localhost:8080/vcs/recipes/1/branches
   
   # Verify user authentication
   curl -H "Authorization: Bearer <token>" http://localhost:8080/vcs/recipes/1/init
   ```

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.recipefy.version: DEBUG
    org.springframework.data.mongodb: DEBUG
    org.hibernate.SQL: DEBUG
```

### Health Diagnostics

```bash
# Check all health indicators
curl http://localhost:8080/actuator/health

# Check database connectivity
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/mongo

# Verify data consistency
curl http://localhost:8080/vcs/recipes/1/branches
```

## Integration with Recipe Service

### Recipe Initialization Flow

1. Recipe Service calls `/vcs/recipes/{id}/init` after creating recipe metadata
2. Version Service creates main branch and initial commit
3. Recipe content stored in MongoDB as first snapshot
4. Branch metadata stored in PostgreSQL for fast queries

### Copy Recipe Flow

1. Recipe Service initiates recipe copy with source branch ID
2. Version Service copies branch structure and content
3. New branch created for copied recipe with preserved history
4. Recipe Service receives new branch information
