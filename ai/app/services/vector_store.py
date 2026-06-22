from typing import Optional
import chromadb
from langchain_chroma import Chroma
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain.schema import Document
from app.config import settings


class VectorStoreManager:

    _instance: Optional["VectorStoreManager"] = None

    def __init__(self):
        self.embeddings = GoogleGenerativeAIEmbeddings(
            model="models/embedding-001",
            google_api_key=settings.google_api_key,
        )
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
