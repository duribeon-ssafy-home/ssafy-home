import logging
import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.api import health, rag
from app.api.deps import get_rag_service
from app.exceptions.handlers import register_exception_handlers

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("RAG 서비스 초기화 시작...")
    service = get_rag_service()
    docs_dir = os.path.join(os.path.dirname(__file__), "..", "data", "documents")
    if os.path.exists(docs_dir):
        count = service.initialize_from_directory(docs_dir)
        logger.info(f"문서 {count}개 청크 인덱싱 완료")
    yield
    logger.info("RAG 서비스 종료")


app = FastAPI(title="SSAFY Home RAG Service", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

register_exception_handlers(app)
app.include_router(health.router)
app.include_router(rag.router)
