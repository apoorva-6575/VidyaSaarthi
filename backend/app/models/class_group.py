from sqlalchemy import Column, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base
from app.models.learner import learner_class_association

class ClassGroup(Base):
    __tablename__ = "classes"
    
    id = Column(String, primary_key=True, index=True)
    join_code = Column(String, unique=True, index=True, nullable=False)
    name = Column(String, nullable=False)
    grade = Column(String, nullable=True)
    subject = Column(String, nullable=True)
    teacher_id = Column(String, ForeignKey("teachers.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    teacher = relationship("Teacher", back_populates="classes")
    learners = relationship("Learner", secondary=learner_class_association, back_populates="classes")
