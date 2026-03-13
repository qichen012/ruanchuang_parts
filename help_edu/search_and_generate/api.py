from openai import OpenAI
import os
def get_bocha_api_key():
    bocha_api_key = os.getenv("BOCHA_API_KEY")
    if not bocha_api_key:
        env_path = os.path.join(os.path.dirname(__file__), ".env")
        if os.path.exists(env_path):
            with open(env_path, "r") as f:
                for line in f:
                    if line.strip().startswith("BOCHA_API_KEY"):
                        bocha_api_key = line.split("=")[1].strip()
                        break
    return bocha_api_key

def get_qwen_client():
    qwen_api_key = os.getenv("DASHSCOPE_API_KEY")
    if not qwen_api_key:
        env_path = os.path.join(os.path.dirname(__file__), ".env")
        if os.path.exists(env_path):
            with open(env_path, "r") as f:
                for line in f:
                    if line.strip().startswith("DASHSCOPE_API_KEY"):
                        qwen_api_key = line.split("=")[1].strip()
                        break
    if not qwen_api_key:
        return None
    
    ase_url = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    return OpenAI(api_key=qwen_api_key, base_url=ase_url)
