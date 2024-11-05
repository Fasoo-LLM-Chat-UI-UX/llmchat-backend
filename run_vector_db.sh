#!/bin/bash

# Remove existing container if exists
docker rm -f chroma-db || true

# Run Chroma DB container
docker run -d \
  --name chroma-db \
  -p 8000:8000 \
  -v chroma-data:/chroma/chroma \
  --restart unless-stopped \
  chromadb/chroma:latest