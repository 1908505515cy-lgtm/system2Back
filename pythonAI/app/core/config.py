class Settings:
    PROJECT_NAME: str = "AI 产业大脑 - 算法中枢"

    OLLAMA_MODEL: str = "gemma4:latest"
    OLLAMA_BASE_URL: str = "http://127.0.0.1:11434"

    MODEL_NUM_CTX: int = 8192
    MODEL_TEMPERATURE: float = 0.0

    JAVA_SERVICE_BASE_URL: str = "http://127.0.0.1:9096"

    # Java 回调共享密钥（需与 Spring Boot 的 ai.callback.secret 一致）
    AI_CALLBACK_SECRET: str = "System2AiCallbackSecret2026"

    LOG_LEVEL: str = "INFO"


settings = Settings()