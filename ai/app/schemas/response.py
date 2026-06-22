from pydantic import BaseModel
from typing import Generic, TypeVar, Optional

T = TypeVar("T")


class SourceCitation(BaseModel):
    source: str
    content: str


class ChatResponse(BaseModel):
    answer: str
    sources: list[SourceCitation]
    query: str


class ApiResponse(BaseModel, Generic[T]):
    success: bool
    message: str
    data: Optional[T] = None
    error_code: Optional[str] = None

    @classmethod
    def ok(cls, message: str, data: T) -> "ApiResponse[T]":
        return cls(success=True, message=message, data=data)

    @classmethod
    def fail(cls, message: str, error_code: str) -> "ApiResponse[None]":
        return cls(success=False, message=message, error_code=error_code)
