from sqlalchemy import Column, String, DateTime, Integer, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base

class ClassMaterialAssignment(Base):
    __tablename__ = "class_materials"
    
    id = Column(String, primary_key=True, index=True)
    class_id = Column(String, ForeignKey("classes.id"), index=True, nullable=False)
    package_id = Column(String, index=True, nullable=False)
    version = Column(Integer, nullable=False)
    teacher_id = Column(String, ForeignKey("teachers.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    class_group = relationship("ClassGroup")
    teacher = relationship("Teacher")
