from fastapi import FastAPI, UploadFile, File
from funasr import AutoModel
import uvicorn
import os
import shutil
import time
import sys

# 添加 Fun-ASR 仓库路径到 Python 路径
FUN_ASR_REPO = "/Users/xwj/Desktop/ruanchuang/FunASR/Fun-ASR"
sys.path.insert(0, FUN_ASR_REPO)

app = FastAPI()

# 定义存储路径
UPLOAD_DIR = "uploaded_audio"
os.makedirs(UPLOAD_DIR, exist_ok=True)

print("⏳ 正在初始化 Fun-ASR-Nano 模型...")

# 本地模型权重路径
LOCAL_MODEL_DIR = "/Users/xwj/Desktop/ruanchuang/FunASR/Fun-ASR-Nano-2512"
# model.py 路径（在 Fun-ASR 仓库根目录）
MODEL_PY_PATH = os.path.join(FUN_ASR_REPO, "model.py")

print(f"📁 模型权重: {LOCAL_MODEL_DIR}")
print(f"📄 model.py: {MODEL_PY_PATH}")
print(f"🔍 model.py 存在: {os.path.exists(MODEL_PY_PATH)}")

try:
    # 加载基础模型
    model = AutoModel(
        model=LOCAL_MODEL_DIR,
        trust_remote_code=True,
        remote_code=MODEL_PY_PATH,
        device="cpu",
        disable_update=True,
    )
    print("✅ 基础模型加载成功！")
    
    # 加载带VAD的模型
    try:
        model_with_vad = AutoModel(
            model=LOCAL_MODEL_DIR,
            trust_remote_code=True,
            remote_code=MODEL_PY_PATH,
            vad_model="fsmn-vad",
            vad_kwargs={"max_single_segment_time": 30000},
            device="cpu",
            disable_update=True,
        )
        print("✅ VAD模型加载成功！")
    except Exception as e:
        print(f"⚠️ VAD模型加载失败: {e}")
        model_with_vad = None
        
except Exception as e:
    print(f"❌ 模型加载失败: {e}")
    import traceback
    traceback.print_exc()
    raise


@app.get("/")
def read_root():
    return {
        "status": "Alive",
        "message": "Fun-ASR-Nano Service Running",
        "model_path": LOCAL_MODEL_DIR,
        "vad_enabled": model_with_vad is not None
    }


@app.post("/upload-audio")
async def upload_audio(
    file: UploadFile = File(...),
    use_vad: bool = False,
    language: str = "中文",
    hotwords: str = ""
):
    file_location = None
    try:
        timestamp = int(time.time())
        original_ext = os.path.splitext(file.filename)[1] or ".wav"
        filename = f"audio_{timestamp}{original_ext}"
        file_location = os.path.join(UPLOAD_DIR, filename)

        with open(file_location, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        file_size = os.path.getsize(file_location)
        print(f"🎤 收到文件: {filename}, 大小: {file_size / 1024:.2f} KB")

        selected_model = model_with_vad if (use_vad and model_with_vad) else model
        hotword_list = [w.strip() for w in hotwords.split(",") if w.strip()]
        
        res = selected_model.generate(
            input=[file_location],
            cache={},
            batch_size=1,
            hotwords=hotword_list if hotword_list else [],
            language=language,
            itn=True,
        )
        
        if res and isinstance(res, list) and len(res) > 0:
            text_result = res[0].get("text", "")
            print(f"🤖 识别成功 [{language}]: {text_result}")
            
            return {
                "success": True,
                "text": text_result,
                "language": language,
                "filename": filename,
                "file_size": file_size,
                "used_vad": use_vad and model_with_vad is not None
            }
        else:
            return {
                "success": True,
                "text": "",
                "message": "未识别到语音内容"
            }

    except Exception as e:
        print(f"❌ 识别错误: {e}")
        import traceback
        traceback.print_exc()
        return {
            "success": False,
            "error": str(e)
        }
    finally:
        if file_location and os.path.exists(file_location):
            try:
                os.remove(file_location)
            except:
                pass


@app.post("/transcribe")
async def transcribe_local_file(
    audio_path: str,
    use_vad: bool = False,
    language: str = "中文",
    hotwords: str = ""
):
    try:
        if not os.path.exists(audio_path):
            return {"success": False, "error": "文件不存在"}
        
        print(f"🎤 转录: {audio_path}")
        
        selected_model = model_with_vad if (use_vad and model_with_vad) else model
        hotword_list = [w.strip() for w in hotwords.split(",") if w.strip()]
        
        res = selected_model.generate(
            input=[audio_path],
            cache={},
            batch_size=1,
            hotwords=hotword_list if hotword_list else [],
            language=language,
            itn=True,
        )
        
        if res and len(res) > 0:
            text_result = res[0].get("text", "")
            return {
                "success": True,
                "text": text_result,
                "language": language
            }
        else:
            return {
                "success": True,
                "text": "",
                "message": "未识别到语音内容"
            }
            
    except Exception as e:
        print(f"❌ 转录错误: {e}")
        import traceback
        traceback.print_exc()
        return {
            "success": False,
            "error": str(e)
        }


if __name__ == "__main__":
    print("\n🚀 启动服务...")
    print("📍 地址: http://localhost:8000")
    print("📖 文档: http://localhost:8000/docs\n")
    
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="info")