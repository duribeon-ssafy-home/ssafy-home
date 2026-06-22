import pytest
from fastapi.testclient import TestClient
from unittest.mock import MagicMock
from app.schemas.response import ChatResponse, SourceCitation
from app.api.deps import get_rag_service
from app.main import app


@pytest.fixture
def client():
    mock_service = MagicMock()
    mock_service.chat.return_value = ChatResponse(
        answer="계약 갱신 청구권은 2년 연장 권리입니다.",
        sources=[SourceCitation(source="임대차보호법.md", content="제6조의3...")],
        query="계약 갱신 청구권이 뭐야?",
    )
    mock_service.add_documents.return_value = 3
    mock_service.initialize_from_directory.return_value = 0

    app.dependency_overrides[get_rag_service] = lambda: mock_service
    yield TestClient(app)
    app.dependency_overrides.clear()


def test_health_check(client):
    resp = client.get("/api/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    assert data["data"]["status"] == "UP"


def test_chat_endpoint(client):
    resp = client.post("/api/rag/chat", json={"question": "계약 갱신 청구권이 뭐야?"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    assert "계약 갱신" in data["data"]["answer"]
    assert len(data["data"]["sources"]) == 1


def test_chat_empty_question_fails(client):
    resp = client.post("/api/rag/chat", json={"question": ""})
    assert resp.status_code == 422


def test_add_document_endpoint(client):
    resp = client.post("/api/rag/documents", json={
        "content": "임대차보호법 내용입니다. 계약 갱신 청구권 관련 조항.",
        "metadata": {"source": "test.md"}
    })
    assert resp.status_code == 201
    data = resp.json()
    assert data["success"] is True
    assert data["data"]["chunk_count"] == 3
