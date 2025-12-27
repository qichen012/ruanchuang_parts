from fastapi import FastAPI
from pydantic import BaseModel
from main_brain import run_super_brain # 导入刚才写的逻辑

app = FastAPI()

class RequestData(BaseModel):
    text: str

@app.post("/ask_brain")
async def ask_brain(data: RequestData):
    # 调用超级大脑逻辑
    result = run_super_brain(data.text)
    return result

# 启动命令: uvicorn server:app --host 0.0.0.0 --port 8000