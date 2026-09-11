from sqlalchemy import Column, String, DateTime, Integer, JSON
from sqlalchemy.sql import func
from app.db.base_class import Base

class ContentPackage(Base):
    __tablename__ = "content_packages"
    
    id = Column(String, primary_key=True, index=True) # package_id
    version = Column(Integer, primary_key=True)
    subject = Column(String, index=True)
    grade = Column(String, index=True)
    language = Column(String, index=True)
    checksum = Column(String, nullable=False)
    size = Column(Integer, nullable=False)
    status = Column(String, default="DRAFT", index=True) # DRAFT, PUBLISHED, REVOKED
    metadata_json = Column(JSON, nullable=True) # Extra manifest info
    created_at = Column(DateTime(timezone=True), server_default=func.now())
