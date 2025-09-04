from sqlalchemy.orm import Session
from typing import List, Optional
from fastapi import UploadFile
import uuid
import os
import shutil
from datetime import datetime
from app.models.document import Document
from app.schemas.document import DocumentCreate, DocumentUpdate
from app.core.config import settings

class DocumentService:
    def __init__(self, db: Session):
        self.db = db

    def get_document(self, document_id: int) -> Optional[Document]:
        """Get document by ID"""
        return self.db.query(Document).filter(Document.id == document_id).first()

    def get_user_documents(self, user_id: uuid.UUID, skip: int = 0, limit: int = 100) -> List[Document]:
        """Get documents created by a specific user"""
        return self.db.query(Document).filter(
            Document.created_by == user_id
        ).offset(skip).limit(limit).all()

    def get_public_documents(self, skip: int = 0, limit: int = 100) -> List[Document]:
        """Get all public documents"""
        return self.db.query(Document).filter(
            Document.is_public == True
        ).offset(skip).limit(limit).all()

    def upload_document(self, file: UploadFile, title: str, description: Optional[str], is_public: bool, user_id: uuid.UUID) -> Document:
        """Upload a new document"""
        file_extension = file.filename.split(".")[-1] if file.filename else ""
        unique_filename = f"{uuid.uuid4()}.{file_extension}"

        file_path = os.path.join(settings.UPLOAD_DIR, unique_filename)

        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        file_size = os.path.getsize(file_path)

        
        db_document = Document(
            title=title,
            description=description,
            file_path=unique_filename,
            file_name=file.filename or "unknown",
            file_size=file_size,
            file_type=file_extension,
            is_public=is_public,
            created_by=user_id
        )

        self.db.add(db_document)
        self.db.commit()
        self.db.refresh(db_document)

        return db_document

    def update_document(self, document_id: int, document_update: DocumentUpdate, user_id: uuid.UUID) -> Optional[Document]:
        """Update document"""
        db_document = self.get_document(document_id)
        if not db_document:
            return None

        if db_document.created_by != user_id:
            return None

        update_data = document_update.dict(exclude_unset=True)

        for field, value in update_data.items():
            setattr(db_document, field, value)

        self.db.commit()
        self.db.refresh(db_document)
        return db_document

    def delete_document(self, document_id: int, user_id: uuid.UUID) -> bool:
        """Delete document"""
        db_document = self.get_document(document_id)
        if not db_document:
            return False

        if db_document.created_by != user_id:
            return False

        file_path = os.path.join(settings.UPLOAD_DIR, db_document.file_path)
        if os.path.exists(file_path):
            os.remove(file_path)

        self.db.delete(db_document)
        self.db.commit()
        return True

    def search_documents(self, query: str, user_id: uuid.UUID, skip: int = 0, limit: int = 100) -> List[Document]:
        """Search documents by title or description"""
        return self.db.query(Document).filter(
            (Document.created_by == user_id) &
            ((Document.title.contains(query)) | (Document.description.contains(query)))
        ).offset(skip).limit(limit).all()

    def get_document_stats(self, user_id: uuid.UUID) -> dict:
        """Get document statistics for a user"""
        total_documents = self.db.query(Document).filter(Document.created_by == user_id).count()
        public_documents = self.db.query(Document).filter(
            Document.created_by == user_id,
            Document.is_public == True
        ).count()
        total_size = self.db.query(Document.file_size).filter(Document.created_by == user_id).all()
        total_size = sum(size[0] for size in total_size) if total_size else 0

        return {
            "total_documents": total_documents,
            "public_documents": public_documents,
            "private_documents": total_documents - public_documents,
            "total_size_bytes": total_size,
            "total_size_mb": round(total_size / (1024 * 1024), 2)
        } 