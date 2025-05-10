# Stage 1: Build the React application (using Vite)
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

# Build the application using Vite
# The build script might be 'build', 'vite build', etc. Check your package.json.
# Assuming 'npm run build' which typically calls 'vite build'
RUN npm run build

# Stage 2: Serve the static files using Nginx
FROM nginx:stable-alpine

# Copy the built static files from the builder stage to Nginx's web root
COPY --from=builder /app/dist /usr/share/nginx/html

# (Optional) Copy a custom Nginx configuration if needed
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 (Nginx default)
EXPOSE 80

# Command to start Nginx
CMD ["nginx", "-g", "daemon off;"]