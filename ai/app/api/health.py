from fastapi import APIRouter
from app.schemas.response import ApiResponse

router = APIRouter(tags=["System"])


@router.get("/api/health", response_model=ApiResponse[dict])
async def health_check():
    return ApiResponse.ok("서비스가 정상 동작 중입니다.", {"status": "UP", "service": "rag-service"})
