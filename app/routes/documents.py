from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from typing import List, Optional
import os
from fastapi.responses import JSONResponse
from app.core.database import get_db
from app.core.config import settings
from app.models.user import User
from app.schemas.document import (
    Document, DocumentCreate, DocumentUpdate,
    DocumentTemplate, DocumentTemplateCreate, DocumentTemplateUpdate,
    DocumentGenerate
)
from app.services.document_service import DocumentService
from app.services.template_service import TemplateService
from app.routes.auth import get_current_user

router = APIRouter()

@router.post("/upload", response_model=Document)
async def upload_document(
    file: UploadFile = File(...),
    title: str = Form(...),
    description: Optional[str] = Form(None),
    is_public: bool = Form(False),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload a new document"""
    if file.size and file.size > settings.MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="File too large"
        )

    file_extension = file.filename.split(".")[-1].lower() if file.filename else ""
    allowed_extensions = settings.ALLOWED_EXTENSIONS.split(",")
    if file_extension not in allowed_extensions:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File type not allowed. Allowed types: {', '.join(allowed_extensions)}"
        )

    document_service = DocumentService(db)
    return document_service.upload_document(file, title, description, is_public, current_user.id)

@router.get("/", response_model=List[Document])
def get_documents(
    skip: int = 0,
    limit: int = 100,
    public_only: bool = False,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get documents"""
    document_service = DocumentService(db)
    if public_only:
        return document_service.get_public_documents(skip=skip, limit=limit)
    else:
        return document_service.get_user_documents(current_user.id, skip=skip, limit=limit)

@router.get("/{document_id}", response_model=Document)
def get_document(
    document_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get document by ID"""
    document_service = DocumentService(db)
    document = document_service.get_document(document_id)

    if document is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )

    if not document.is_public and document.created_by != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )

    return document



@router.get("/{document_id}/download")
def download_document(
    document_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Download document file"""
    document_service = DocumentService(db)
    document = document_service.get_document(document_id)

    if document is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )

    if not document.is_public and document.created_by != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )

    file_path = os.path.join(settings.UPLOAD_DIR, document.file_path)
    if not os.path.exists(file_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="File not found"
        )

    return FileResponse(
        file_path,
        filename=document.file_name,
        media_type='application/octet-stream'
    )

@router.put("/{document_id}", response_model=Document)
def update_document(
    document_id: int,
    document_update: DocumentUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update document"""
    document_service = DocumentService(db)
    document = document_service.update_document(document_id, document_update, current_user.id)

    if document is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )

    return document

@router.delete("/{document_id}")
def delete_document(
    document_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete document"""
    document_service = DocumentService(db)
    success = document_service.delete_document(document_id, current_user.id)

    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )

    return {"message": "Document deleted successfully"}

@router.post("/templates", response_model=DocumentTemplate)
def create_template(
    template: DocumentTemplateCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new document template"""
    template_service = TemplateService(db)
    return template_service.create_template(template, current_user.id)

@router.get("/templates", response_model=List[DocumentTemplate])
def get_templates(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get document templates"""
    template_service = TemplateService(db)
    return template_service.get_templates(skip=skip, limit=limit)

@router.get("/templates/{template_id}", response_model=DocumentTemplate)
def get_template(
    template_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get template by ID"""
    template_service = TemplateService(db)
    template = template_service.get_template(template_id)

    if template is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Template not found"
        )

    return template

@router.put("/templates/{template_id}", response_model=DocumentTemplate)
def update_template(
    template_id: int,
    template_update: DocumentTemplateUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update template"""
    template_service = TemplateService(db)
    return template_service.update_template(template_id, template_update, current_user.id)

@router.delete("/templates/{template_id}")
def delete_template(
    template_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete template"""
    template_service = TemplateService(db)
    success = template_service.delete_template(template_id, current_user.id)

    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Template not found"
        )

    return {"message": "Template deleted successfully"}

@router.post("/templates/{template_id}/generate", response_model=Document)
def generate_document(
    template_id: int,
    generate_request: DocumentGenerate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate document from template"""
    template_service = TemplateService(db)

    template = template_service.get_template(template_id)
    if template is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Template not found"
        )

    document = template_service.generate_document(
        template,
        generate_request.variables,
        generate_request.output_format,
        current_user.id
    )

    return document 