from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Rural EdTech Platform"
    
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "postgres"
    POSTGRES_DB: str = "edtech_db"
    DATABASE_URL: str = "postgresql://postgres:postgres@db:5432/edtech_db"
    
    MINIO_ENDPOINT: str = "minio:9000"
    MINIO_ACCESS_KEY: str = "admin"
    MINIO_SECRET_KEY: str = "password123"
    MINIO_SECURE: bool = False
    
    JWT_SECRET: str = "super-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7 # 7 days
    
    BACKEND_CORS_ORIGINS: list[str] = ["*"] # Configure properly for production

    class Config:
        env_file = ".env"

settings = Settings()
