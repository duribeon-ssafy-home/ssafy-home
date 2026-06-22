from typing import Optional
import httpx
import chromadb
from langchain_chroma import Chroma
from langchain_core.embeddings import Embeddings
from langchain.schema import Document
from app.config import settings

_EMBED_BASE = "https://generativelanguage.googleapis.com/v1beta"
_EMBED_MODEL = "models/gemini-embedding-001"


class GeminiRESTEmbeddings(Embeddings):
    """gRPC 없이 REST API로 Gemini 임베딩을 호출하는 클래스."""

    def __init__(self, api_key: str):
        self.api_key = api_key

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        requests = [
            {
                "model": _EMBED_MODEL,
                "content": {"parts": [{"text": t}]},
                "taskType": "RETRIEVAL_DOCUMENT",
            }
            for t in texts
        ]
        resp = httpx.post(
            f"{_EMBED_BASE}/{_EMBED_MODEL}:batchEmbedContents",
            params={"key": self.api_key},
            json={"requests": requests},
            timeout=60.0,
        )
        resp.raise_for_status()
        return [e["values"] for e in resp.json()["embeddings"]]

    def embed_query(self, text: str) -> list[float]:
        resp = httpx.post(
            f"{_EMBED_BASE}/{_EMBED_MODEL}:embedContent",
            params={"key": self.api_key},
            json={
                "model": _EMBED_MODEL,
                "content": {"parts": [{"text": text}]},
                "taskType": "RETRIEVAL_QUERY",
            },
            timeout=60.0,
        )
        resp.raise_for_status()
        return resp.json()["embedding"]["values"]


class VectorStoreManager:

    _instance: Optional["VectorStoreManager"] = None

    def __init__(self):
        self.embeddings = GeminiRESTEmbeddings(api_key=settings.google_api_key)
        self.client = chromadb.PersistentClient(path=settings.chroma_persist_dir)
        self.vector_store = Chroma(
            client=self.client,
            collection_name=settings.chroma_collection_name,
            embedding_function=self.embeddings,
        )

    @classmethod
    def get_instance(cls) -> "VectorStoreManager":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def get_retriever(self):
        return self.vector_store.as_retriever(
            search_type="mmr",
            search_kwargs={"k": settings.retriever_top_k, "fetch_k": 20},
        )

    def add_documents(self, documents: list[Document]) -> list[str]:
        return self.vector_store.add_documents(documents)
