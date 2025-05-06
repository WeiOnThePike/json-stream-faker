#!/bin/bash
set -e

# Script to publish the JSON Stream Faker Docker image to a repository
# Usage: ./publish-docker.sh [options]
#
# Options:
#   --version VERSION   Specify the version tag (default: latest)
#   --registry URL      Specify the registry URL (default: none for Docker Hub)
#   --repository REPO   Specify the repository name (default: jsonstreamfaker)
#   --username USER     Specify the registry username
#   --password PASS     Specify the registry password (use environment variable instead for security)
#   --local             Push to local registry (defaults to localhost:5000)
#   --help              Show this help message

VERSION="latest"
REGISTRY=""
REPOSITORY="jsonstreamfaker"
USERNAME=""
PASSWORD=""
LOCAL=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    --version)
      VERSION="$2"
      shift
      shift
      ;;
    --registry)
      REGISTRY="$2"
      shift
      shift
      ;;
    --repository)
      REPOSITORY="$2"
      shift
      shift
      ;;
    --username)
      USERNAME="$2"
      shift
      shift
      ;;
    --password)
      PASSWORD="$2"
      shift
      shift
      ;;
    --local)
      LOCAL=true
      shift
      ;;
    --help)
      echo "Usage: ./publish-docker.sh [options]"
      echo ""
      echo "Options:"
      echo "  --version VERSION   Specify the version tag (default: latest)"
      echo "  --registry URL      Specify the registry URL (default: none for Docker Hub)"
      echo "  --repository REPO   Specify the repository name (default: jsonstreamfaker)"
      echo "  --username USER     Specify the registry username"
      echo "  --password PASS     Specify the registry password (use environment variable instead for security)"
      echo "  --local             Push to local registry (defaults to localhost:5000)"
      echo "  --help              Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $key"
      exit 1
      ;;
  esac
done

# If not built yet, build the Docker image
if [[ "$(docker images -q jsonstreamfaker:latest 2> /dev/null)" == "" ]]; then
  echo "Docker image not found. Building it first..."
  ./build.sh
fi

# Handle local registry
if [ "$LOCAL" = true ]; then
  REGISTRY="localhost:5000"
  echo "Publishing to local registry at $REGISTRY"
  
  # Check if local registry is running, if not start it
  if ! docker ps | grep -q "registry:2"; then
    echo "Local registry not running. Starting it..."
    docker run -d -p 5000:5000 --name registry registry:2
  fi
fi

# Set the full image name with registry if specified
if [ -n "$REGISTRY" ]; then
  TARGET_IMAGE="$REGISTRY/$REPOSITORY:$VERSION"
else
  TARGET_IMAGE="$REPOSITORY:$VERSION"
fi

echo "Tagging image as $TARGET_IMAGE"
docker tag jsonstreamfaker:latest $TARGET_IMAGE

# Login to registry if username is provided
if [ -n "$USERNAME" ]; then
  if [ -n "$PASSWORD" ]; then
    echo "Logging in to registry with provided credentials"
    echo "$PASSWORD" | docker login $REGISTRY -u $USERNAME --password-stdin
  elif [ -n "$DOCKER_PASSWORD" ]; then
    echo "Logging in to registry with credentials from environment variable"
    echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u $USERNAME --password-stdin
  else
    echo "Logging in to registry (password will be prompted)"
    docker login $REGISTRY -u $USERNAME
  fi
fi

# Push the image
echo "Pushing image to $TARGET_IMAGE"
docker push $TARGET_IMAGE

echo "Successfully published image to $TARGET_IMAGE"

# If we logged in, log out for security
if [ -n "$USERNAME" ]; then
  if [ -n "$REGISTRY" ]; then
    docker logout $REGISTRY
  else
    docker logout
  fi
fi

echo "Done!"