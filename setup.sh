#!/bin/bash

# Setup Script for AgentFlow (Mac/Linux)

echo -e "\033[0;36m--- AgentFlow Setup ---\033[0m"

# 1. Check for Docker
if ! command -v docker &> /dev/null; then
    echo -e "\033[0;31m[!] Docker not found. Please install Docker Desktop (Mac/Windows) or Docker Engine (Linux).\033[0m"
    exit 1
fi

# 2. Create models directory
if [ ! -d "./models" ]; then
    mkdir -p "./models"
    echo -e "\033[0;32m[+] Created models directory\033[0m"
fi

# 3. Download Llama 3.2 3B (GGUF) if not present
MODEL_PATH="./models/model.gguf"
if [ ! -f "$MODEL_PATH" ]; then
    echo -e "\033[0;33m[*] Model not found. Downloading Llama 3.2 3B (GGUF)...\033[0m"
    # Using a reliable HF URL for Llama 3.2 3B GGUF
    URL="https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
    
    if command -v curl &> /dev/null; then
        curl -L "$URL" -o "$MODEL_PATH"
    elif command -v wget &> /dev/null; then
        wget -O "$MODEL_PATH" "$URL"
    else
        echo -e "\033[0;31m[!] Neither curl nor wget found. Please download the model manually to ./models/model.gguf\033[0m"
        exit 1
    fi
    echo -e "\033[0;32m[+] Download complete!\033[0m"
else
    echo -e "\033[0;32m[+] Model already exists\033[0m"
fi

# 4. Start Docker Compose
echo -e "\033[0;36m[*] Starting AgentFlow via Docker Compose...\033[0m"
docker-compose up --build
