# Stage 1: Build the application
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package.json and package-lock.json (or yarn.lock)
COPY package*.json ./
# If using yarn, uncomment next line and comment out npm ci
# COPY yarn.lock ./

# Install dependencies
RUN npm ci
# If using yarn, uncomment next line and comment out npm ci
# RUN yarn install --frozen-lockfile

# Copy the rest of the application code
COPY . .

# Build the application
RUN npm run build

# Stage 2: Create the production image
FROM node:20-alpine

WORKDIR /app

# Copy only necessary files from the builder stage
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package*.json ./
# If using yarn, uncomment next line
# COPY --from=builder /app/yarn.lock ./

# Set environment variables (can be overridden in docker-compose.yml or at runtime)
ENV NODE_ENV production
ENV PORT 3000
# ENV LLM_API_KEY your_llm_api_key_here # Better to set this in docker-compose or runtime
# ENV JAVA_GENERATOR_URL http://java-generator-service:8080

EXPOSE ${PORT}

# Command to run the application
CMD ["node", "dist/main.js"]