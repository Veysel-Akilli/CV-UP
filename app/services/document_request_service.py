from sqlalchemy.orm import Session
from sqlalchemy import func  
from typing import List, Optional
import uuid

from app.models.document_request import DocumentRequest
from app.schemas.document_request import DocumentRequestCreate, DocumentRequestUpdate


def _model_dump(obj):
    """
    Pydantic v1/v2 uyum katmanı:
    - v1: obj.dict(exclude_unset=True)
    - v2: obj.model_dump(exclude_unset=True)
    """
    if hasattr(obj, "model_dump"):
        return obj.model_dump(exclude_unset=True)  
    return obj.dict(exclude_unset=True)  


class DocumentRequestService:
    def __init__(self, db: Session):
        self.db = db

    def get_document_request(self, request_id: uuid.UUID) -> Optional[DocumentRequest]:
        """Get document request by ID"""
        return (
            self.db.query(DocumentRequest)
            .filter(DocumentRequest.id == request_id)
            .first()
        )

    def get_user_document_requests(
        self, user_id: uuid.UUID, skip: int = 0, limit: int = 100
    ) -> List[DocumentRequest]:
        """Get all document requests for a user with pagination"""
        return (
            self.db.query(DocumentRequest)
            .filter(DocumentRequest.user_id == user_id)
            .order_by(DocumentRequest.created_at.desc())
            .offset(skip)
            .limit(limit)
            .all()
        )

    def get_document_requests_by_type(
        self, user_id: uuid.UUID, document_type: str, skip: int = 0, limit: int = 100
    ) -> List[DocumentRequest]:
        """Get document requests by type for a user"""
        return (
            self.db.query(DocumentRequest)
            .filter(
                DocumentRequest.user_id == user_id,
                DocumentRequest.document_type == document_type,
            )
            .order_by(DocumentRequest.created_at.desc())
            .offset(skip)
            .limit(limit)
            .all()
        )

    def create_document_request(
        self, document_request: DocumentRequestCreate, user_id: uuid.UUID
    ) -> DocumentRequest:
        """Create a new document request"""
        db_document_request = DocumentRequest(
            user_id=user_id,
            document_type=document_request.document_type,
            input_data=document_request.input_data,
            generated_content=document_request.generated_content,
        )
        self.db.add(db_document_request)
        self.db.commit()
        self.db.refresh(db_document_request)
        return db_document_request

    def update_document_request(
        self, request_id: uuid.UUID, document_request_update: DocumentRequestUpdate
    ) -> Optional[DocumentRequest]:
        """Update document request"""
        db_document_request = self.get_document_request(request_id)
        if not db_document_request:
            return None

        update_data = _model_dump(document_request_update)
        for field, value in update_data.items():
            setattr(db_document_request, field, value)

        self.db.commit()
        self.db.refresh(db_document_request)
        return db_document_request

    def delete_document_request(self, request_id: uuid.UUID) -> bool:
        """Delete document request"""
        db_document_request = self.get_document_request(request_id)
        if not db_document_request:
            return False

        self.db.delete(db_document_request)
        self.db.commit()
        return True

    def get_document_request_stats(self, user_id: uuid.UUID) -> dict:
        """Get statistics for user's document requests"""
        total_requests = (
            self.db.query(func.count(DocumentRequest.id))
            .filter(DocumentRequest.user_id == user_id)
            .scalar()
        )

        rows = (
            self.db.query(
                DocumentRequest.document_type,
                func.count(DocumentRequest.id),
            )
            .filter(DocumentRequest.user_id == user_id)
            .group_by(DocumentRequest.document_type)
            .all()
        )

        requests_by_type = {doc_type: count for doc_type, count in rows}

        return {
            "total_requests": int(total_requests or 0),
            "requests_by_type": requests_by_type,
        }
