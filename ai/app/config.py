from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    google_api_key: str
    host: str = "0.0.0.0"
    port: int = 8000
    allowed_origins: list[str] = ["http://localhost:8080", "http://localhost:5173"]
    chroma_persist_dir: str = "./chroma_db"
    chroma_collection_name: str = "legal_documents"
    chunk_size: int = 800
    chunk_overlap: int = 150
    retriever_top_k: int = 5

    model_config = {"env_file": ".env"}


settings = Settings()
