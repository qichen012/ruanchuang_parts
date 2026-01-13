"""Device control utilities via Accessibility Service (HTTP)."""

import time
import requests
from typing import Optional
import urllib.parse
from phone_agent.config.apps import APP_PACKAGES
from phone_agent.config.timing import TIMING_CONFIG

# 你的手机 IP 地址 (确保手机和电脑在同一个 WiFi)
# 建议在路由器给手机设个静态IP，或者每次运行前改一下
PHONE_IP = "10.29.8.38" 
BASE_URL = f"http://{PHONE_IP}:8080"

def _send_cmd(endpoint: str, params: dict, device_id: str | None = None, delay: float | None = None):
    # 1. 确定目标 IP
    target_ip = device_id if device_id else "10.29.8.38" # 你的默认IP
    
    # 2. 🚨 关键修复：手动构建 URL 以确保中文被编码
    # requests 库通常会自动处理，但为了排除万一，我们手动拼装
    query_string = urllib.parse.urlencode(params)
    url = f"http://{target_ip}:8080/{endpoint}?{query_string}"
    
    try:
        # print(f"📡 Sending: {url}") # 调试用
        
        # 注意：这里不再传 params=params，而是直接请求拼装好的 URL
        response = requests.get(url, timeout=5)
        
        # 3. 检查响应，如果非 200，打印出来
        if response.status_code != 200:
            print(f"❌ Server Error {response.status_code}: {response.text}")

    except Exception as e:
        print(f"❌ Command Failed: {e}")
    
    time.sleep(delay if delay is not None else 0.5)

def get_current_app(device_id: str | None = None) -> str:
    """
    通过 HTTP 询问 App：现在谁在前台？
    """
    try:
        resp = requests.get(f"{BASE_URL}/info/current_package", timeout=2)
        if resp.status_code == 200:
            current_pkg = resp.text.strip() # 比如 "com.tencent.mm"
            
            # 反向查找 App 名字
            for app_name, package in APP_PACKAGES.items():
                if package == current_pkg:
                    return app_name
    except:
        pass
    return "System Home"

def tap(
    x: int, y: int, device_id: str | None = None, delay: float | None = None
) -> None:
    _send_cmd("action", {"type": "tap", "x": x, "y": y}, 
              delay, TIMING_CONFIG.device.default_tap_delay)

def double_tap(
    x: int, y: int, device_id: str | None = None, delay: float | None = None
) -> None:
    # 既然是无障碍，建议直接发一个 "double_tap" 指令给手机，
    # 让手机自己处理两次点击，比网络来回两次更稳。
    _send_cmd("action", {"type": "double_tap", "x": x, "y": y}, 
              delay, TIMING_CONFIG.device.default_double_tap_delay)

def long_press(
    x: int, y: int, duration_ms: int = 1000, device_id: str | None = None, delay: float | None = None
) -> None:
    # 无障碍服务可以直接处理长按
    _send_cmd("action", {"type": "long_press", "x": x, "y": y, "duration": duration_ms}, 
              delay, TIMING_CONFIG.device.default_long_press_delay)

def swipe(
    start_x: int, start_y: int, end_x: int, end_y: int,
    duration_ms: int | None = None, device_id: str | None = None, delay: float | None = None
) -> None:
    if duration_ms is None:
        duration_ms = 1000 # 默认滑动时间
        
    _send_cmd("action", {
        "type": "swipe", 
        "x1": start_x, "y1": start_y, 
        "x2": end_x, "y2": end_y, 
        "duration": duration_ms
    }, delay, TIMING_CONFIG.device.default_swipe_delay)

def back(device_id: str | None = None, delay: float | None = None) -> None:
    _send_cmd("action", {"type": "global", "code": "back"}, 
              delay, TIMING_CONFIG.device.default_back_delay)

def home(device_id: str | None = None, delay: float | None = None) -> None:
    _send_cmd("action", {"type": "global", "code": "home"}, 
              delay, TIMING_CONFIG.device.default_home_delay)

def launch_app(
    app_name: str, device_id: str | None = None, delay: float | None = None
) -> bool:
    if app_name not in APP_PACKAGES:
        return False
        
    package = APP_PACKAGES[app_name]
    
    # 这一步很关键：Android App 收到这个请求后，
    # 会调用 context.startActivity(...) 来启动应用
    _send_cmd("action", {"type": "launch", "package": package}, 
              delay, TIMING_CONFIG.device.default_launch_delay)
    return True