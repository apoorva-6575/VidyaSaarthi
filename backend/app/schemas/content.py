from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime

class ContentPackageBase(BaseModel):
    subject: str
    grade: str
    language: str
    checksum: str
    size: int
    status: str = "DRAFT"
    metadata_json: Optional[Dict[str, Any]] = None

class ContentPackageCreate(ContentPackageBase):
    id: str
    version: int

class ContentPackageResponse(ContentPackageBase):
    id: str
    version: int
    created_at: datetime

    class Config:
        from_attributes = True

class ContentPackageManifest(BaseModel):
    package_id: str
    version: int
    subject: str
    grade: str
    languages: List[str]
    checksum: str
    size: int
    dependencies: List[str] = []
