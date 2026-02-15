# Setup Script for AgentFlow

Write-Host "--- AgentFlow Setup ---" -ForegroundColor Cyan

# 1. Check for Docker
if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "[!] Docker not found. Please install Docker Desktop: https://www.docker.com/products/docker-desktop" -ForegroundColor Red
    exit
}

# 2. Create models directory
if (!(Test-Path -Path "./models")) {
    New-Item -ItemType Directory -Path "./models"
    Write-Host "[+] Created models directory" -ForegroundColor Green
}

# 3. Download Llama 3.2 3B (GGUF) if not present
$modelPath = "./models/model.gguf"
if (!(Test-Path -Path $modelPath)) {
    Write-Host "[*] Model not found. Downloading Llama 3.2 3B (GGUF)..." -ForegroundColor Yellow
    # Using a reliable HF URL for Llama 3.2 3B GGUF (e.g., from bartowski or similar)
    $url = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
    Invoke-WebRequest -Uri $url -OutFile $modelPath
    Write-Host "[+] Download complete!" -ForegroundColor Green
} else {
    Write-Host "[+] Model already exists" -ForegroundColor Green
}

# 4. Start Docker Compose
Write-Host "[*] Starting AgentFlow via Docker Compose..." -ForegroundColor Cyan
docker-compose up --build
