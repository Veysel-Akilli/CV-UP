from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from uuid import UUID 

class DocumentBase(BaseModel):
    title: str
    description: Optional[str] = None
    is_public: bool = False
    file_type: str

class DocumentCreate(DocumentBase):
    pass

class DocumentUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    is_public: Optional[bool] = None

class DocumentInDB(DocumentBase):
    id: int
    file_path: str
    file_name: str
    file_size: int
    file_type: str
    created_by: UUID  
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class Document(DocumentInDB):
    pass

class DocumentTemplateBase(BaseModel):
    name: str
    description: Optional[str] = None
    template_content: str
    variables: Optional[str] = None

class DocumentTemplateCreate(DocumentTemplateBase):
    pass

class DocumentTemplateUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    template_content: Optional[str] = None
    variables: Optional[str] = None
    is_active: Optional[bool] = None

class DocumentTemplateInDB(DocumentTemplateBase):
    id: int
    is_active: bool
    created_by: UUID
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class DocumentTemplate(DocumentTemplateInDB):
    pass

class DocumentGenerate(BaseModel):
    template_id: int
    variables: dict
    output_format: str = "pdf"
