from sqlalchemy import Column, String, DateTime, Integer
from sqlalchemy.sql import func
from app.db.base_class import Base

class SyncState(Base):
    __tablename__ = "sync_states"
    
    device_id = Column(String, primary_key=True, index=True)
    last_server_sequence = Column(Integer, default=0)
    last_sync_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
