import os


class Settings:
    PROJECT_NAME: str = "AI 产业大脑 - 算法中枢"

    # Ollama 基础配置
    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")

    # 多模型路由：按任务类型选择模型
    # OLLAMA_MODEL: 默认模型（兜底）
    # TOOL_MODEL: 工具选择/Agent 决策（需要强 JSON 能力，推荐小而快的模型）
    # CHAT_MODEL: 自由对话（推荐大模型，回答质量高）
    OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "gemma4:latest")
    TOOL_MODEL: str = os.getenv("TOOL_MODEL", "")  # 空则使用 OLLAMA_MODEL
    CHAT_MODEL: str = os.getenv("CHAT_MODEL", "")   # 空则使用 OLLAMA_MODEL

    MODEL_NUM_CTX: int = int(os.getenv("MODEL_NUM_CTX", "8192"))
    MODEL_TEMPERATURE: float = float(os.getenv("MODEL_TEMPERATURE", "0.0"))

    JAVA_SERVICE_BASE_URL: str = os.getenv("JAVA_SERVICE_BASE_URL", "http://127.0.0.1:9096")

    # Java 回调共享密钥（需与 Spring Boot 的 ai.callback.secret 一致，必须通过环境变量设置）
    AI_CALLBACK_SECRET: str = os.getenv("AI_CALLBACK_SECRET", "")

    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    @property
    def tool_model(self) -> str:
        """工具选择模型（Agent 决策用）"""
        return self.TOOL_MODEL or self.OLLAMA_MODEL

    @property
    def chat_model(self) -> str:
        """对话模型（自由聊天用）"""
        return self.CHAT_MODEL or self.OLLAMA_MODEL


settings = Settings()
