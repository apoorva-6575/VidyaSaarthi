from sqlalchemy import Column, String, DateTime, ForeignKey, Table
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base

# Association table for many-to-many relationship between learners and classes
learner_class_association = Table(
    "learner_class",
    Base.metadata,
    Column("learner_id", String, ForeignKey("learners.id")),
    Column("class_id", String, ForeignKey("classes.id"))
)

class Learner(Base):
    __tablename__ = "learners"
    
    id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    grade = Column(String, nullable=True)
    preferred_language = Column(String, default="en")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    classes = relationship("ClassGroup", secondary=learner_class_association, back_populates="learners")
    events = relationship("LearningEvent", back_populates="learner")
