import requests
import json
import time

BASE_URL = "http://localhost:8080/api"

def test_stateless():
    print("\n--- Testing Stateless (Effectiveness) ---")
    # Turn 1
    r1 = requests.post(f"{BASE_URL}/generate", json={"prompt": "Hi, my name is Alex. Remember that."})
    print(f"Turn 1 Response: {r1.json().get('response', '').strip()}")
    
    # Turn 2
    r2 = requests.post(f"{BASE_URL}/generate", json={"prompt": "What is my name?"})
    print(f"Turn 2 Response: {r2.json().get('response', '').strip()}")

def test_memory_complex():
    print("\n--- Testing Complex Memory (Effectiveness) ---")
    create_r = requests.post(f"{BASE_URL}/conversations", json={"systemPrompt": "You are a financial analyst agent."})
    conv_id = create_r.json().get("conversationId")
    
    # scenario 1: Instruction Persistence & Formatting
    print("\nScenario 1: Strict Formatting Instruction")
    requests.post(f"{BASE_URL}/conversations/{conv_id}/chat", json={"message": "From now on, only respond in valid JSON format. Start every response with '{'."})
    
    # scenario 2: Data Point A
    print("Scenario 2: Providing Data Point A")
    requests.post(f"{BASE_URL}/conversations/{conv_id}/chat", json={"message": "The revenue for Q1 was $500,000."})
    
    # scenario 3: Data Point B
    print("Scenario 3: Providing Data Point B")
    requests.post(f"{BASE_URL}/conversations/{conv_id}/chat", json={"message": "The expenses for Q1 were $350,000."})
    
    # scenario 4: Synthesis & Instruction Adherence
    print("Scenario 4: Synthesis & Formatting Check")
    t_start = time.time()
    r4 = requests.post(f"{BASE_URL}/conversations/{conv_id}/chat", json={"message": "What was the net profit for Q1? Remember the JSON formatting rule."})
    t_end = time.time()
    
    response = r4.json().get("response", "").strip()
    print(f"Final Response:\n{response}")
    print(f"Latency: {t_end - t_start:.2f}s")
    
    # Quick Validation
    is_json = response.startswith("{") and response.endswith("}")
    has_profit = "150,000" in response or "150000" in response
    print(f"\n[Validation] Followed JSON rule: {'PASS' if is_json else 'FAIL'}")
    print(f"\n[Validation] Correct Calculation ($150k): {'PASS' if has_profit else 'FAIL'}")

if __name__ == "__main__":
    try:
        # test_stateless() # Skipping simple test for speed
        test_memory_complex()
    except Exception as e:
        print(f"Error: {e}. Make sure AgentFlow is running on 8080 and LLM on 8081.")
