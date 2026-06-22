import pytest
from unittest.mock import patch, MagicMock
from langchain.schema import Document


def test_vector_store_singleton():
    from app.services.vector_store import VectorStoreManager
    with patch("app.services.vector_store.GoogleGenerativeAIEmbeddings"), \
         patch("app.services.vector_store.chromadb.PersistentClient"), \
         patch("app.services.vector_store.Chroma"):
        VectorStoreManager._instance = None
        inst1 = VectorStoreManager.get_instance()
        inst2 = VectorStoreManager.get_instance()
        assert inst1 is inst2
        VectorStoreManager._instance = None


def test_add_documents_returns_ids():
    from app.services.vector_store import VectorStoreManager
    mock_vs = MagicMock()
    mock_vs.add_documents.return_value = ["id1", "id2"]
    with patch("app.services.vector_store.GoogleGenerativeAIEmbeddings"), \
         patch("app.services.vector_store.chromadb.PersistentClient"), \
         patch("app.services.vector_store.Chroma", return_value=mock_vs):
        VectorStoreManager._instance = None
        manager = VectorStoreManager.get_instance()
        manager.vector_store = mock_vs
        docs = [Document(page_content="테스트", metadata={"source": "test.md"})]
        ids = manager.add_documents(docs)
        assert ids == ["id1", "id2"]
        VectorStoreManager._instance = None
