from fastapi import APIRouter, Depends, status
from app.schemas.request import ChatRequest, AddDocumentRequest
from app.schemas.response import ApiResponse, ChatResponse
from app.api.deps import get_rag_service
from app.services.rag_service import RAGService

router = APIRouter(prefix="/api/rag", tags=["RAG"])


@router.post("/chat", response_model=ApiResponse[ChatResponse])
async def chat(
    request: ChatRequest,
    rag_service: RAGService = Depends(get_rag_service),
):
    result = rag_service.chat(request.question)
    return ApiResponse.ok("답변을 생성했습니다.", result)


@router.post("/documents", response_model=ApiResponse[dict], status_code=status.HTTP_201_CREATED)
async def add_document(
    request: AddDocumentRequest,
    rag_service: RAGService = Depends(get_rag_service),
):
    chunk_count = rag_service.add_documents(request.content, request.metadata)
    return ApiResponse.ok("문서가 추가되었습니다.", {"chunk_count": chunk_count})
