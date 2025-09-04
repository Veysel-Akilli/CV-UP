from pydantic import BaseModel
from typing import Optional, Dict, Any
from datetime import datetime
import uuid

class DocumentRequestBase(BaseModel):
    document_type: str
    input_data: Optional[Dict[str, Any]] = None
    generated_content: Optional[str] = None

class DocumentRequestCreate(DocumentRequestBase):
    pass

class DocumentRequestUpdate(BaseModel):
    document_type: Optional[str] = None
    input_data: Optional[Dict[str, Any]] = None
    generated_content: Optional[str] = None

class DocumentRequestInDB(DocumentRequestBase):
    id: uuid.UUID
    user_id: uuid.UUID
    created_at: datetime

    class Config:
        from_attributes = True

class DocumentRequest(DocumentRequestInDB):
    pass

class DocumentGenerateRequest(BaseModel):
    document_type: str
    input_data: Dict[str, Any] 