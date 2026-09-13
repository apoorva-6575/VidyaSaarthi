from sqlalchemy import Column, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base

class MaterialRequest(Base):
    __tablename__ = "material_requests"
    
    id = Column(String, primary_key=True, index=True)
    class_id = Column(String, ForeignKey("classes.id"), index=True, nullable=False)
    package_id = Column(String, index=True, nullable=False)
    requester_id = Column(String, ForeignKey("learners.id"), nullable=False)
    provider_id = Column(String, nullable=True) # Could be learner or teacher
    status = Column(String, nullable=False, default="PENDING")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    class_group = relationship("ClassGroup")
    requester = relationship("Learner")
