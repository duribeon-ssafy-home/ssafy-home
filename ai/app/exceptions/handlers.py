from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from app.schemas.response import ApiResponse


def register_exception_handlers(app: FastAPI):

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, exc: RequestValidationError):
        errors = [f"{e['loc'][-1]}: {e['msg']}" for e in exc.errors()]
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content=ApiResponse.fail(", ".join(errors), "INVALID_INPUT").model_dump(),
        )

    @app.exception_handler(Exception)
    async def general_error_handler(request: Request, exc: Exception):
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content=ApiResponse.fail("내부 서버 오류가 발생했습니다.", "INTERNAL_ERROR").model_dump(),
        )
