# AgentFlow 🚀

Universal local model interface with persistent memory and OpenAI-compatible history management.

## Installation & Running

### Option 1: Automatic Setup (Recommended for Portability)
Requires: [Docker Desktop](https://www.docker.com/products/docker-desktop)

**Windows:**
1. Open PowerShell in this directory.
2. Run the setup script:
   ```powershell
   .\setup.ps1
   ```

**Mac / Linux:**
1. Open terminal in this directory.
2. Make the script executable and run it:
   ```bash
   chmod +x setup.sh
   ./setup.sh
   ```

*This will automatically download the Llama 3.2 3B model and start the entire stack.*

3. Access the UI at: `http://localhost:8080`

### Option 2: Manual Development Setup
Requires: Java 17, Maven, and `llama.cpp` server running on port 8081.

1. Start your `llama-server.exe` on port 8081.
2. Build and run the Java app:
   ```powershell
   mvn spring-boot:run
   ```

## Key Features
- **Universal LLM Compatibility**: Uses the OpenAI-compatible Chat Completions API.
- **Context Window Management**: Sliding window memory prevents prompt overflow.
- **Conversation Persistence**: Memory survives restarts (enable via `memory.type=file`).
- **User Preference Memory**: Automatically extracts and persists user preferences into a persistent system prompt.

## Configuration
Modify `src/main/resources/application.properties` to tune memory behavior:
- `memory.type`: Set to `file` for persistence.
- `memory.max-messages`: Size of context window.
- `memory.summarize-threshold`: When to trigger LLM-based summarization.
