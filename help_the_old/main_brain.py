import json
from zhipuai import ZhipuAI

# 1. 初始化 GLM-4 客户端
client = ZhipuAI(api_key="69cee8e59f2a4e44af21c06c0ee57871.fJjJ5mye1L3WFmmh") # 记得替换 Key

# 2. 定义 Sub-Agents 的能力 (工具定义)
# 这是告诉大脑：你有两个手下，他们分别能干什么，需要什么参数。
tools = [
    {
        "type": "function",
        "function": {
            "name": "dispatch_work_task",
            "description": "处理与工作、会议、企业微信、邮件相关的任务。当用户提到老板、开会、日报、代码、监听消息时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "task_type": {"type": "string", "enum": ["schedule_meeting", "monitor_chat", "summarize_report"], "description": "任务类型"},
                    "content": {"type": "string", "description": "具体的任务内容或关键词"},
                    "priority": {"type": "string", "enum": ["high", "medium", "low"], "description": "任务优先级"}
                },
                "required": ["task_type", "content"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "dispatch_life_task",
            "description": "处理与生活、娱乐、购物、出行相关的任务。当用户提到打车、点外卖、买票、刷视频时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "app_name": {"type": "string", "description": "需要操作的App名称，如美团、滴滴、抖音"},
                    "action_instruction": {"type": "string", "description": "给AutoGLM的具体自然语言指令，例如'帮我点一杯瑞幸咖啡'"}
                },
                "required": ["app_name", "action_instruction"]
            }
        }
    }
]

# 3. 模拟从 MemoBase 获取的用户画像 (Memory)
user_profile = """
{
    "name": "Alex",
    "job": "Software Engineer",
    "preferences": {
        "coffee": "Iced Americano",
        "transport": "Didi Premier",
        "work_style": "Deep Focus in the morning"
    },
    "sensitive_keywords": ["紧急", "服务器宕机", "老板@我"]
}
"""

# 4. 超级大脑主函数
def run_super_brain(user_input):
    messages = [
        {
            "role": "system",
            "content": f"""
            你是一个基于多Agent架构的'超级手机助手'的大脑。
            
            你的核心职责是：
            1. 分析用户的自然语言输入或系统通知。
            2. 结合用户画像(User Profile)进行决策。
            3. 将任务精准分发给 'Work_Agent' (工作) 或 'Life_Agent' (生活)。
            
            当前用户画像数据：
            {user_profile}
            
            注意：如果是闲聊，请直接回复文本。如果是明确的任务，必须调用工具。
            """
        },
        {"role": "user", "content": user_input}
    ]

    print(f"🧠 大脑正在思考: {user_input}")

    response = client.chat.completions.create(
        model="glm-4.7", # 使用最强的 GLM-4 模型
        messages=messages,
        tools=tools,
        tool_choice="auto", # 让模型自己决定是用工具还是直接聊天
    )

    # 5. 解析大脑的决定
    choice = response.choices[0].message
    
    # 检查是否有工具调用 (Function Call)
    if choice.tool_calls:
        for tool_call in choice.tool_calls:
            function_name = tool_call.function.name
            function_args = json.loads(tool_call.function.arguments)
            
            print(f"⚡️ 触发动作分发 -> [{function_name}]")
            print(f"📦 参数内容: {json.dumps(function_args, ensure_ascii=False, indent=2)}")
            
            # 这里是实际对接你的 App 逻辑的地方
            # return build_json_response(function_name, function_args)
            return {"status": "action_triggered", "agent": function_name, "data": function_args}
    else:
        # 如果只是普通闲聊
        reply = choice.content
        print(f"💬 大脑回复: {reply}")
        return {"status": "chat", "reply": reply}

# --- 测试用例 ---

# 测试 1: 工作场景 (监听模式)
print("\n--- Test Case 1: Work ---")
run_super_brain("我要开始工作了，帮我盯着企业微信，如果老板@我或者提到'开会'，就提醒我。")

# 测试 2: 生活场景 (结合用户画像)
print("\n--- Test Case 2: Life ---")
run_super_brain("有点困了，帮我点杯咖啡。") 
# GLM-4 应该能根据画像自动补全 '冰美式' 的信息给 Life Agent