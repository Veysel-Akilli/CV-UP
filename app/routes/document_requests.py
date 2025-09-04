from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from typing import List
import uuid

from app.core.database import get_db
from app.models.user import User
from app.schemas.document_request import DocumentRequest, DocumentRequestCreate, DocumentRequestUpdate, DocumentGenerateRequest
from app.services.document_request_service import DocumentRequestService
from app.routes.auth import get_current_user
from app.services.gemini_service import GeminiService

router = APIRouter()

@router.get("/", response_model=List[DocumentRequest])
def get_document_requests(
    skip: int = 0,
    limit: int = 100,
    document_type: str = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get all document requests for the current user"""
    document_request_service = DocumentRequestService(db)
    
    if document_type:
        return document_request_service.get_document_requests_by_type(
            current_user.id, document_type, skip=skip, limit=limit
        )
    else:
        return document_request_service.get_user_document_requests(
            current_user.id, skip=skip, limit=limit
        )

@router.get("/{request_id}", response_model=DocumentRequest)
def get_document_request(
    request_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific document request"""
    document_request_service = DocumentRequestService(db)
    document_request = document_request_service.get_document_request(request_id)
    
    if document_request is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document request not found"
        )
    
    if document_request.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    return document_request

@router.post("/", response_model=DocumentRequest)
def create_document_request(
    document_request: DocumentRequestCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new document request"""
    document_request_service = DocumentRequestService(db)
    return document_request_service.create_document_request(document_request, current_user.id)

@router.put("/{request_id}", response_model=DocumentRequest)
def update_document_request(
    request_id: uuid.UUID,
    document_request_update: DocumentRequestUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a document request"""
    document_request_service = DocumentRequestService(db)
    
    existing_request = document_request_service.get_document_request(request_id)
    if existing_request is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document request not found"
        )
    
    if existing_request.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    updated_request = document_request_service.update_document_request(request_id, document_request_update)
    if updated_request is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document request not found"
        )
    
    return updated_request

@router.delete("/{request_id}")
def delete_document_request(
    request_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a document request"""
    document_request_service = DocumentRequestService(db)
    
    existing_request = document_request_service.get_document_request(request_id)
    if existing_request is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document request not found"
        )
    
    if existing_request.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    success = document_request_service.delete_document_request(request_id)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document request not found"
        )
    
    return {"message": "Document request deleted successfully"}

@router.get("/stats/summary")
def get_document_request_stats(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get statistics for user's document requests"""
    document_request_service = DocumentRequestService(db)
    return document_request_service.get_document_request_stats(current_user.id) 

@router.post("/generate", status_code=status.HTTP_200_OK)
async def generate_document(request: DocumentGenerateRequest):
    """Gemini API ile belge içeriği üret"""
    generated_content = await GeminiService.generate_content(request.document_type, request.input_data)
    return {
        "document_type": request.document_type,
        "input_data": request.input_data,
        "generated_content": generated_content
    } 

@router.get("/documents", response_model=List[DocumentRequest])
def list_user_documents(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Kullanıcının oluşturduğu tüm belge taleplerini listeler"""
    service = DocumentRequestService(db)
    return service.get_user_document_requests(current_user.id)

@router.get("/documents/{request_id}", response_model=DocumentRequest)
def get_user_document(
    request_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Kullanıcının tekil belge talebini getirir"""
    service = DocumentRequestService(db)
    doc = service.get_document_request(request_id)
    if not doc or doc.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Belge bulunamadı veya erişim yok.")
    return doc

@router.delete("/documents/{request_id}")
def delete_user_document(
    request_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Kullanıcının bir belge talebini siler"""
    service = DocumentRequestService(db)
    doc = service.get_document_request(request_id)
    if not doc or doc.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Belge bulunamadı veya erişim yok.")
    service.delete_document_request(request_id)
    return {"message": "Belge başarıyla silindi."} 