#!/usr/bin/env python3
"""
Accessibility Agent CLI - Dedicated entry point for OPPO Accessibility Mode.

Usage:
    python accessibility_main.py [OPTIONS]
"""

import argparse
import os
import sys
import requests
from openai import OpenAI

# 导入核心组件
from phone_agent import PhoneAgent
from phone_agent.agent import AgentConfig
from phone_agent.model import ModelConfig
from phone_agent.device_factory import DeviceType, set_device_type, get_device_factory
from phone_agent.config.apps import list_supported_apps

# 你的无障碍 App 默认端口
DEFAULT_APP_PORT = 8080

def check_accessibility_connection(device_ip: str, port: int = DEFAULT_APP_PORT) -> bool:
    """
    专门检查无障碍 App 是否在线
    """
    print(f"🔍 Checking Accessibility App Connection ({device_ip}:{port})...")
    print("-" * 50)
    
    url = f"http://{device_ip}:{port}/ping"
    try:
        print(f"1. Pinging App at {url}...", end=" ")
        # 设置短超时，演示时如果不通立即报错，不浪费时间
        response = requests.get(url, timeout=3)
        
        if response.status_code == 200:
            print("✅ OK")
            print(f"   Response: {response.text.strip()}")
            print("-" * 50)
            print("✅ Device Connection Verified!\n")
            return True
        else:
            print(f"❌ FAILED (Status: {response.status_code})")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ FAILED")
        print("   Error: Connection refused.")
        print("   Solution:")
        print("     1. Ensure the OPPO phone and PC are on the same WiFi.")
        print("     2. Ensure the 'OPPO Accessibility App' is running and Server is started.")
        print(f"     3. Check if phone IP is actually {device_ip}.")
        return False
    except Exception as e:
        print(f"❌ FAILED ({e})")
        return False

def check_model_api(base_url: str, model_name: str, api_key: str = "EMPTY") -> bool:
    """
    保持官方的模型检查逻辑，确保大脑在线
    """
    print("🔍 Checking Model API...")
    print("-" * 50)
    print(f"1. Checking connectivity ({base_url})...", end=" ")
    try:
        client = OpenAI(base_url=base_url, api_key=api_key, timeout=10.0)
        # 发送一个极简请求测试连通性
        client.chat.completions.create(
            model=model_name,
            messages=[{"role": "user", "content": "Hi"}],
            max_tokens=1
        )
        print("✅ OK")
        print("-" * 50)
        print("✅ Model Brain Verified!\n")
        return True
    except Exception as e:
        print("❌ FAILED")
        print(f"   Error: {e}")
        print("   Tip: Ensure GLM-4 server is running or API key is correct.")
        return False

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="OPPO Accessibility Agent - Pure WiFi Mode"
    )

    # 模型配置
    parser.add_argument("--base-url", type=str, default=os.getenv("PHONE_AGENT_BASE_URL", "http://localhost:8000/v1"))
    parser.add_argument("--model", type=str, default=os.getenv("PHONE_AGENT_MODEL", "autoglm-phone-9b"))
    parser.add_argument("--apikey", type=str, default=os.getenv("PHONE_AGENT_API_KEY", "EMPTY"))
    
    # 设备配置 (关键不同点：这里主要接收 IP)
    parser.add_argument("--device-ip", type=str, default="192.168.1.101", help="Phone IP address")
    parser.add_argument("--port", type=int, default=DEFAULT_APP_PORT, help="App HTTP Server port")
    
    # 任务配置
    parser.add_argument("--max-steps", type=int, default=50)
    parser.add_argument("--task", type=str, help="Task to execute immediately")
    parser.add_argument("--lang", type=str, choices=["cn", "en"], default="cn")
    parser.add_argument("--quiet", "-q", action="store_true")

    return parser.parse_args()

def main():
    args = parse_args()

    # 1. 强制设置全局设备类型为 ACCESSIBILITY
    # 这是最关键的一步，告诉工厂类加载我们写的 HTTP 驱动
    set_device_type(DeviceType.ACCESSIBILITY)

    # 2. 打印欢迎信息
    print("=" * 60)
    print(" 🚀 OPPO Accessibility Agent Starting...")
    print("    Mode: Wireless / No-Root / Pure HTTP")
    print(f"    Target: {args.device_ip}:{args.port}")
    print("=" * 60)
    print()

    # 3. 执行检查 (跳过了 ADB 检查，只查 HTTP 和 模型)
    if not check_accessibility_connection(args.device_ip, args.port):
        sys.exit(1)
        
    if not check_model_api(args.base_url, args.model, args.apikey):
        sys.exit(1)

    # 4. 初始化 Agent
    # 注意：我们将 IP 地址传给 device_id，因为在 HTTP 驱动里，IP 就是 ID
    model_config = ModelConfig(
        base_url=args.base_url,
        model_name=args.model,
        api_key=args.apikey,
        lang=args.lang,
    )

    agent_config = AgentConfig(
        max_steps=args.max_steps,
        device_id=args.device_ip, # 关键：这里传入 IP
        verbose=not args.quiet,
        lang=args.lang,
    )

    agent = PhoneAgent(
        model_config=model_config,
        agent_config=agent_config,
    )

    # 5. 运行主循环
    if args.task:
        print(f"\n📝 Executing Task: {args.task}\n")
        result = agent.run(args.task)
        print(f"\n✨ Result: {result}")
    else:
        print("\n🎙️  Ready for commands. Type 'quit' to exit.\n")
        while True:
            try:
                task = input("User: ").strip()
                if task.lower() in ("quit", "exit", "q"):
                    print("Bye!")
                    break
                if not task:
                    continue
                
                result = agent.run(task)
                print(f"\n🤖 Agent: {result}\n")
                agent.reset()
                
            except KeyboardInterrupt:
                print("\nInterrupted.")
                break
            except Exception as e:
                print(f"\n⚠️ Error: {e}\n")

if __name__ == "__main__":
    main()