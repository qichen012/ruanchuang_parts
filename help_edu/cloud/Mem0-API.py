import requests
import os
import json
from datetime import datetime

def set_memory_configuration():
    """设置 Mem0 记忆配置"""
    try:
        response = requests.post("http://localhost:8888/configure")
        print('设置记忆配置成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 设置记忆配置失败: {e}")
        return None


def upload_memory(role:str, content:str, user_id:str, agent_id:str, run_id:str):
    """上传记忆到 Mem0"""
    data = {
        "messages": [
            {
                "role": role,
                "content": content
            }
        ],
        "user_id": user_id,
        "agent_id": agent_id,
        "run_id": run_id,
        "metadata": {
            "additionalProp1": {}
        }
    }
    # 构建上传数据
    try:
        requests.post("http://localhost:8888/memories", json=data)
        print('上传记忆成功')
    except Exception as e:
        print(f"[Error] 上传记忆失败: {e}")
        return None


def read_all_memories(user_id:str, agent_id:str, run_id:str):
    """从 Mem0 读取所有记忆"""
    # 构建读取数据
    try:
        data = {
            "user_id": user_id,
            "agent_id": agent_id,
            "run_id": run_id
        }
        response = requests.get("http://localhost:8888/memories", json=data)
    except Exception as e:
        print(f"[Error] 读取所有记忆失败: {e}")
        return None
    print('读取所有记忆成功')
    return response.json()


def read_single_memory(memory_id:str):
    """从 Mem0 读取单个记忆"""
    try:
        response = requests.get(f"http://localhost:8888/memories/{memory_id}")
        print('读取单个记忆成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 读取单个记忆失败: {e}")
        return None


def delete_all_memories(user_id:str, agent_id:str, run_id:str):
    """从 Mem0 删除所有记忆"""
    data = {
        "user_id": user_id,
        "agent_id": agent_id,
        "run_id": run_id
    }
    try:
        response = requests.delete("http://localhost:8888/v1/memories", json=data)
        print('删除所有记忆成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 删除所有记忆失败: {e}")
        return None


def delete_single_memory(memory_id:str):
    """从 Mem0 删除单个记忆"""
    try:
        response = requests.delete(f"http://localhost:8888/memories/{memory_id}")
        print('删除单个记忆成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 删除单个记忆失败: {e}")
        return None


def update_memory(memory_id:str, new_content:str):
    """更新 Mem0 中的记忆"""
    data = {
        "content": new_content
    }
    try:
        response = requests.put(f"http://localhost:8888/memories/{memory_id}", json=data)
        print('更新记忆成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 更新记忆失败: {e}")
        return None


def search_memory(query:str, user_id:str, agent_id:str, run_id:str):
    """从 Mem0 搜索记忆"""
    data = {
        "query": query,
        "user_id": user_id,
        "run_id": run_id,
        "agent_id": agent_id,
        "filters": {
            "additionalProp1": {}
        }
    }
    try:
        response = requests.post("http://localhost:8888/search", json=data)
        print('搜索记忆成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 搜索记忆失败: {e}")
        return None


def get_memory_history(memory_id:str):
    """从 Mem0 获取记忆历史"""
    try:
        response = requests.get(f"http://localhost:8888/memories/{memory_id}/history")
        print('获取记忆历史成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 获取记忆历史失败: {e}")
        return None


def reset_all_memories():
    try:
        response = requests.post("http://localhost:8888/reset")
        print('重置所有记忆成功')
        return response.json()
    except Exception as e:
        print(f"[Error] 重置所有记忆失败: {e}")
        return None
