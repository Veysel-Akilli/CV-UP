from sqlalchemy.orm import Session
from typing import List, Optional, Dict
import json
import uuid
import os
from datetime import datetime

from app.models.document import DocumentTemplate, Document
from app.schemas.document import DocumentTemplateCreate, DocumentTemplateUpdate
from app.core.config import settings

class TemplateService:
    def __init__(self, db: Session):
        self.db = db

    def get_template(self, template_id: int) -> Optional[DocumentTemplate]:
        """Get template by ID"""
        return self.db.query(DocumentTemplate).filter(DocumentTemplate.id == template_id).first()

    def get_templates(self, skip: int = 0, limit: int = 100) -> List[DocumentTemplate]:
        """Get all active templates"""
        return self.db.query(DocumentTemplate).filter(
            DocumentTemplate.is_active == True
        ).offset(skip).limit(limit).all()

    def get_user_templates(self, user_id: uuid.UUID, skip: int = 0, limit: int = 100) -> List[DocumentTemplate]:
        """Get templates created by a specific user"""
        return self.db.query(DocumentTemplate).filter(
            DocumentTemplate.created_by == user_id,
            DocumentTemplate.is_active == True
        ).offset(skip).limit(limit).all()

    def create_template(self, template: DocumentTemplateCreate, user_id: uuid.UUID) -> DocumentTemplate:
        """Create a new template"""
        db_template = DocumentTemplate(
            name=template.name,
            description=template.description,
            template_content=template.template_content,
            variables=template.variables,
            created_by=user_id
        )

        self.db.add(db_template)
        self.db.commit()
        self.db.refresh(db_template)
        return db_template

    def update_template(self, template_id: int, template_update: DocumentTemplateUpdate, user_id: uuid.UUID) -> Optional[DocumentTemplate]:
        """Update template"""
        db_template = self.get_template(template_id)
        if not db_template:
            return None

        if db_template.created_by != user_id:
            return None

        update_data = template_update.dict(exclude_unset=True)

        for field, value in update_data.items():
            setattr(db_template, field, value)

        self.db.commit()
        self.db.refresh(db_template)
        return db_template

    def delete_template(self, template_id: int, user_id: uuid.UUID) -> bool:
        """Delete template (soft delete by setting is_active to False)"""
        db_template = self.get_template(template_id)
        if not db_template:
            return False

        if db_template.created_by != user_id:
            return False

        db_template.is_active = False
        self.db.commit()
        return True

    def generate_document(self, template: DocumentTemplate, variables: Dict, output_format: str, user_id: uuid.UUID) -> Document:
        """Generate document from template"""
        content = self._process_template(template.template_content, variables)

        filename = f"{template.name}_{uuid.uuid4()}.{output_format}"
        file_path = os.path.join(settings.UPLOAD_DIR, filename)

        if output_format.lower() == "txt":
            self._create_text_file(content, file_path)
        elif output_format.lower() == "html":
            self._create_html_file(content, file_path)
        else:
            self._create_text_file(content, file_path)
            output_format = "txt"

        file_size = os.path.getsize(file_path)

        db_document = Document(
            title=f"Generated from {template.name}",
            description=f"Document generated from template: {template.name}",
            file_path=filename,
            file_name=filename,
            file_size=file_size,
            file_type=output_format,
            is_public=False,
            created_by=user_id
        )

        self.db.add(db_document)
        self.db.commit()
        self.db.refresh(db_document)

        return db_document

    def _process_template(self, template_content: str, variables: Dict) -> str:
        """Process template content by replacing variables"""
        content = template_content

        for key, value in variables.items():
            placeholder = f"{{{{{key}}}}}"
            content = content.replace(placeholder, str(value))

        return content

    def _create_text_file(self, content: str, file_path: str):
        """Create a text file with the given content"""
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

    def _create_html_file(self, content: str, file_path: str):
        """Create an HTML file with the given content"""
        html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Generated Document</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }}
        .content {{ white-space: pre-wrap; }}
    </style>
</head>
<body>
    <div class="content">{content}</div>
</body>
</html>
        """
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(html_content)

    def validate_template_variables(self, template_content: str, variables: Dict) -> Dict:
        """Validate template variables and return missing ones"""
        import re

        pattern = r'\{\{(\w+)\}\}'
        required_variables = set(re.findall(pattern, template_content))

        provided_variables = set(variables.keys())

        missing_variables = required_variables - provided_variables

        return {
            "required": list(required_variables),
            "provided": list(provided_variables),
            "missing": list(missing_variables),
            "is_valid": len(missing_variables) == 0
        } 