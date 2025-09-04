from fastapi import APIRouter, Depends
from typing import Dict, Any
from .auth import require_token  
from app.services.gemini_service import GeminiService

router = APIRouter(prefix="/cv", tags=["cv"])

@router.post("/generate")
async def generate_cv(payload: Dict[str, Any], token: str = Depends(require_token)):
    """
    payload: mobilin gönderdiği fieldValues map'i (aynı anahtar isimleriyle).
    """
    text = await GeminiService.generate_content("cv", payload or {})
    return {"text": text}
