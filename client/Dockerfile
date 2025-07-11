# ──────────────────────────────────────────────────────────
# 1️⃣  Build stage
# ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

# Install pnpm
RUN corepack enable && \
    corepack prepare pnpm@latest --activate

# Create app directory
WORKDIR /app

# Accept build arguments for Vite environment variables
ARG VITE_API_GATEWAY_URL
ARG VITE_AUTH_SERVICE_URL

# Set environment variables from build args
ENV VITE_API_GATEWAY_URL=$VITE_API_GATEWAY_URL
ENV VITE_AUTH_SERVICE_URL=$VITE_AUTH_SERVICE_URL

# Copy only the files needed to install dependencies
COPY package.json pnpm-lock.yaml ./

# Install deps (prod + dev) with frozen lockfile for repeatability
RUN pnpm install --frozen-lockfile

# Copy the rest of the source code
COPY . .

# Build Vite app
RUN pnpm run build

# ──────────────────────────────────────────────────────────
# 2️⃣  Production runtime stage
# ──────────────────────────────────────────────────────────
FROM nginx:alpine AS runner

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 3000 to match the original port
EXPOSE 3000

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
