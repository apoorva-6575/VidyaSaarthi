from sqlalchemy import Column, String, DateTime, ForeignKey, Integer, JSON
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base

class LearningEvent(Base):
    __tablename__ = "learning_events"
    
    id = Column(String, primary_key=True, index=True) # event_id from device
    learner_id = Column(String, ForeignKey("learners.id"), nullable=False, index=True)
    device_id = Column(String, nullable=False, index=True)
    event_type = Column(String, nullable=False, index=True)
    timestamp = Column(DateTime(timezone=True), nullable=False)
    payload = Column(JSON, nullable=False)
    schema_version = Column(Integer, default=1)
    
    # Server side tracking
    server_sequence = Column(Integer, autoincrement=True, unique=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    learner = relationship("Learner", back_populates="events")
