from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    provider: str
    model: str


class SttResponse(BaseModel):
    text: str
    durationMs: int
