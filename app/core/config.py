from pydantic_settings import BaseSettings
from typing import Optional
import os

class Settings(BaseSettings):
    POSTGRES_DB: str = "Project1"
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "145145Kl145"
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: str = "5432"
    
    DATABASE_URL: Optional[str] = None

    SECRET_KEY: str = "145145Kl145"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    APP_NAME: str = "Project1"
    DEBUG: bool = True
    ENVIRONMENT: str = "development"

    MAX_FILE_SIZE: int = 10485760 
    UPLOAD_DIR: str = "./uploads"
    ALLOWED_EXTENSIONS: str = "pdf,doc,docx,txt,rtf"

    GEMINI_API_KEY: str = ""

    @property
    def get_database_url(self) -> str:
        """Construct database URL from individual components"""
        if self.DATABASE_URL:
            return self.DATABASE_URL
        return f"postgresql://{self.POSTGRES_USER}:{self.POSTGRES_PASSWORD}@{self.POSTGRES_HOST}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}"

    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()

os.makedirs(settings.UPLOAD_DIR, exist_ok=True) 