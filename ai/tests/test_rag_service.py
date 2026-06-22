import pytest
from unittest.mock import patch, MagicMock
from langchain.schema import Document
from app.schemas.response import ChatResponse


def test_chat_returns_chat_response():
    from app.services.rag_service import RAGService
    with patch.object(RAGService, "__init__", lambda self: None):
        service = RAGService.__new__(RAGService)
        mock_chain = MagicMock()
        mock_chain.invoke.return_value = {
            "answer": "전세사기 예방을 위해 등기부등본을 확인하세요.",
            "source_docs": [
                Document(page_content="등기부등본 확인 필요", metadata={"source": "가이드.md"})
            ],
            "question": "전세사기 어떻게 피해?",
        }
        service._chain = mock_chain
        result = service.chat("전세사기 어떻게 피해?")
        assert isinstance(result, ChatResponse)
        assert "등기부등본" in result.answer
        assert len(result.sources) == 1
        assert result.sources[0].source == "가이드.md"


def test_add_documents_returns_chunk_count():
    from app.services.rag_service import RAGService
    with patch.object(RAGService, "__init__", lambda self: None):
        service = RAGService.__new__(RAGService)
        mock_loader = MagicMock()
        mock_loader.load_from_text.return_value = [MagicMock(), MagicMock()]
        mock_vsm = MagicMock()
        mock_vsm.add_documents.return_value = ["id1", "id2"]
        service.loader = mock_loader
        service.vs_manager = mock_vsm
        count = service.add_documents("법률 내용", {"source": "법률.md"})
        assert count == 2


def test_duplicate_sources_deduplicated():
    from app.services.rag_service import RAGService
    with patch.object(RAGService, "__init__", lambda self: None):
        service = RAGService.__new__(RAGService)
        mock_chain = MagicMock()
        mock_chain.invoke.return_value = {
            "answer": "답변",
            "source_docs": [
                Document(page_content="내용1", metadata={"source": "같은파일.md"}),
                Document(page_content="내용2", metadata={"source": "같은파일.md"}),
            ],
            "question": "질문",
        }
        service._chain = mock_chain
        result = service.chat("질문")
        assert len(result.sources) == 1
