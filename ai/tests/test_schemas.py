from app.schemas.request import ChatRequest, AddDocumentRequest
from app.schemas.response import ApiResponse, ChatResponse, SourceCitation
import pytest
from pydantic import ValidationError


def test_chat_request_valid():
    req = ChatRequest(question="전세사기 피하는 법")
    assert req.question == "전세사기 피하는 법"


def test_chat_request_empty_fails():
    with pytest.raises(ValidationError):
        ChatRequest(question="")


def test_api_response_ok():
    resp = ApiResponse.ok("성공", {"key": "value"})
    assert resp.success is True
    assert resp.data == {"key": "value"}


def test_api_response_fail():
    resp = ApiResponse.fail("오류", "ERR_001")
    assert resp.success is False
    assert resp.error_code == "ERR_001"
    assert resp.data is None


def test_chat_response():
    citation = SourceCitation(source="임대차보호법.md", content="제6조 내용...")
    resp = ChatResponse(answer="답변입니다", sources=[citation], query="질문")
    assert len(resp.sources) == 1
