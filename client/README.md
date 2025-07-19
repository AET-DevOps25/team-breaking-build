# Client Application

A modern React TypeScript frontend application for the Recipefy platform, providing an intuitive interface for recipe management, version control, and AI-powered recipe assistance.

## Overview

The client application is built with React 18, TypeScript, and Vite, featuring a responsive design with TailwindCSS. It integrates with the backend microservices to provide comprehensive recipe management capabilities including creation, editing, version control, and AI-powered recipe suggestions.

## Key Features

### 🍳 Recipe Management
- **Create & Edit Recipes**: Rich form interface with ingredients, steps, and metadata
- **Recipe Discovery**: Browse and search through recipe collections
- **Image Support**: Upload and display recipe thumbnails
- **Tagging System**: Organize recipes with custom tags

### 🔄 Version Control
- **Recipe History**: Track changes across recipe versions
- **Branching**: Create feature branches for recipe modifications
- **Commit System**: Version recipes with descriptive commit messages
- **Change Visualization**: View differences between recipe versions

### 🤖 AI Integration
- **Recipe Chat**: Interactive chatbot for recipe suggestions and cooking advice
- **Smart Suggestions**: AI-powered recipe recommendations
- **Ingredient-based Generation**: Create recipes from available ingredients
- **Recipe Modification**: AI assistance for dietary adaptations
- **Search with No Hassle**: Search recipe's in the vectorized database rather than dealing with filters.

### 🔐 Authentication
- **OAuth2 Integration**: Secure login via Keycloak
- **User Profiles**: Personal recipe collections and preferences
- **Session Management**: Secure token-based authentication

## Technology Stack

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite (fast development and optimized builds)
- **Styling**: TailwindCSS with custom design system
- **UI Components**: Radix UI primitives with custom styling
- **Forms**: React Hook Form with Zod validation
- **Routing**: React Router v6
- **State Management**: React Context API
- **HTTP Client**: Native fetch with custom API layer
- **Testing**: Vitest with jsdom environment
- **Package Manager**: pnpm

## Project Structure

```
client/
├── public/                 # Static assets
│   ├── favicon.ico
│   ├── health.json        # Health check endpoint
│   └── hero.png           # Landing page assets
├── src/
│   ├── components/        # Reusable UI components
│   │   ├── ui/           # Base UI components (buttons, inputs, etc.)
│   │   ├── chatbot/      # AI chatbot components
│   │   ├── home/         # Landing page components
│   │   ├── navigation/   # Navigation and layout components
│   │   ├── recipe/       # Recipe-specific components
│   │   └── user/         # User profile components
│   ├── contexts/         # React Context providers
│   │   └── AuthContext.tsx
│   ├── hooks/            # Custom React hooks
│   ├── lib/              # Utility libraries and configurations
│   ├── pages/            # Route components
│   ├── services/         # API service layers
│   └── styles/           # Global styles and CSS
├── nginx.conf            # Production nginx configuration
├── Dockerfile            # Multi-stage container build
├── package.json          # Dependencies and scripts
└── vite.config.ts        # Vite configuration
```

## Development Setup

### Prerequisites

- Node.js v20 or higher
- pnpm package manager
- Backend services running (see main project README)

### Installation

```bash
# Install dependencies
pnpm install

# Start development server
pnpm dev
```

The application will be available at [http://localhost:3000](http://localhost:3000).

### Environment Configuration

Create environment variables for development:

```bash
# Development API endpoints (handled by Vite proxy)
VITE_API_GATEWAY_URL=http://localhost:8090
VITE_AUTH_SERVICE_URL=http://localhost:8089
```

## Available Scripts

```bash
# Development
pnpm dev              # Start development server with hot reload
pnpm build            # Build for production
pnpm start            # Preview production build locally

# Testing
pnpm test             # Run tests once
pnpm test:watch       # Run tests in watch mode
```

## API Integration

The client integrates with multiple backend services:

### API Gateway (`/api/*`)
- Recipe CRUD operations
- User management
- Version control operations
- GenAI operations

### Authentication Service (`/auth/*`)
- Login/logout functionality
- User registration
- Token management

### Proxy Configuration

Development proxy routes (configured in `vite.config.ts`):
```typescript
proxy: {
  '/api': 'http://localhost:8090',  // API Gateway
  '/auth': 'http://localhost:8089', // Keycloak Service
}
```

## Key Components

### Authentication Flow
```typescript
// AuthContext provides authentication state
const { login, logout, isAuthenticated, user } = useAuth();
```

### Recipe Management
- `RecipeForm`: Create/edit recipes with validation
- `RecipeCard`: Display recipe summary and metadata
- `RecipeDetail`: Full recipe view with actions
- `RecipeHistory`: Version control visualization

### AI Chatbot
- `Chatbot`: Floating chat interface
- Real-time AI recipe generation and search assistance
- Integration with recipe creation flow

### Navigation
- `NavBar`: Main navigation with authentication state
- `NavLinks`: Dynamic navigation based on user status

## Styling System

### TailwindCSS Configuration
- Custom color palette based on `#FF7C75` (rose/coral theme)
- Responsive design utilities
- Component-specific styling patterns

### Custom Components
- Design system based on Radix UI primitives
- Consistent spacing and typography
- Accessible form controls and interactions

## Testing

### Test Setup
- **Framework**: Vitest with jsdom environment
- **Components**: React Testing Library patterns
- **Coverage**: Unit tests for critical components

### Running Tests
```bash
# Run all tests
pnpm test

# Watch mode for development
pnpm test:watch
```

## Production Deployment

### Docker Build

```bash
# Build production image
docker build -t recipefy-client .

# Run container
docker run -p 3000:3000 recipefy-client
```

### Multi-stage Dockerfile
1. **Builder Stage**: Node.js with pnpm for dependency installation and build
2. **Runtime Stage**: Nginx Alpine for serving static files

### Nginx Configuration
- Custom configuration for SPA routing
- API proxy rules for backend services
- Health check endpoint
- Security headers and asset caching

## Performance Optimizations

- **Code Splitting**: Route-based lazy loading
- **Asset Optimization**: Vite's built-in optimizations
- **Bundle Analysis**: Source maps for debugging
- **Caching**: Nginx-based static asset caching

## Development Guidelines

### Code Organization
- Feature-based component organization
- Shared UI components in `/components/ui/`
- API services separated by domain
- Custom hooks for reusable logic

### Type Safety
- Full TypeScript coverage
- API response type definitions
- Form validation with Zod schemas
- Strict TypeScript configuration

### Best Practices
- React functional components with hooks
- Proper error boundary implementation
- Accessible UI components
- Performance-optimized re-renders

## Integration with Backend

### Authentication Flow
1. User initiates login via Keycloak
2. Frontend receives JWT tokens
3. Tokens included in API requests
4. Automatic token refresh handling

### Recipe Operations
1. Form validation on client side
2. API calls to Recipe Service via Gateway
3. Version control integration for recipe changes
4. Real-time updates via optimistic UI patterns

### AI Integration
1. Chatbot interface for user queries
2. API calls to GenAI service
3. Recipe suggestions integrated into creation flow
4. Context-aware recipe modifications

## Troubleshooting

### Common Development Issues

1. **Port Conflicts**
   ```bash
   # Change port in vite.config.ts or use:
   pnpm dev --port 3001
   ```

2. **API Connection Issues**
   - Verify backend services are running
   - Check proxy configuration in `vite.config.ts`
   - Confirm environment variables

3. **Build Failures**
   ```bash
   # Clear node_modules and reinstall
   rm -rf node_modules pnpm-lock.yaml
   pnpm install
   ```

### Production Issues

1. **Nginx Routing Issues**
   - Verify `nginx.conf` SPA configuration
   - Check API proxy rules

2. **Asset Loading Problems**
   - Confirm build output in `/dist`
   - Verify nginx asset serving configuration

## Security Considerations

- XSS protection via React's built-in escaping
- CSRF protection through token-based authentication
- Secure API communication via HTTPS in production
- Content Security Policy headers via nginx
- Input validation and sanitization
