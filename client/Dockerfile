# ──────────────────────────────────────────────────────────
# 1️⃣  Build stage
# ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

# Install pnpm
RUN corepack enable && \
    corepack prepare pnpm@latest --activate

# Create app directory
WORKDIR /app

# Copy only the files needed to install dependencies
COPY package.json pnpm-lock.yaml ./

# Install deps (prod + dev) with frozen lockfile for repeatability
RUN pnpm install --frozen-lockfile

# Copy the rest of the source code
COPY . .

# Build Next.js
RUN pnpm exec next build

# ──────────────────────────────────────────────────────────
# 2️⃣  Production runtime stage
# ──────────────────────────────────────────────────────────
FROM node:20-alpine AS runner

# Install pnpm so `pnpm exec next` works
RUN corepack enable && \
    corepack prepare pnpm@latest --activate

WORKDIR /app

# Copy only what’s needed to run the server
COPY --from=builder /app/package.json .
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/pnpm-lock.yaml ./
# Install *production* deps only
RUN pnpm install --prod --frozen-lockfile --ignore-scripts

# Expose Next.js default port
EXPOSE 3000

# Start the server
CMD ["pnpm", "exec", "next", "start"]
