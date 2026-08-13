import os
import threading
from typing import Any

from google import genai


_CLIENT_ENV_LOCK = threading.Lock()


def create_gemini_client(api_key: str) -> Any:
    """Build a client from GEMINI_API_KEY even if GOOGLE_API_KEY exists."""
    with _CLIENT_ENV_LOCK:
        google_api_key = os.environ.pop("GOOGLE_API_KEY", None)
        try:
            return genai.Client(api_key=api_key)
        finally:
            if google_api_key is not None:
                os.environ["GOOGLE_API_KEY"] = google_api_key
