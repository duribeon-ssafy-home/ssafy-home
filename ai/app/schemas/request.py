from pydantic import BaseModel, Field
from typing import Optional


class ChatRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=1000)
    session_id: Optional[str] = None


class AddDocumentRequest(BaseModel):
    content: str = Field(..., min_length=10)
    metadata: dict = Field(default_factory=dict)
