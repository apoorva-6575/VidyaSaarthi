from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class LearnerBase(BaseModel):
    name: str
    grade: Optional[str] = None
    preferred_language: str = "en"

class LearnerCreate(LearnerBase):
    id: str

class LearnerResponse(LearnerBase):
    id: str
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class ClassGroupBase(BaseModel):
    name: str
    grade: Optional[str] = None
    subject: Optional[str] = None

class ClassGroupCreate(ClassGroupBase):
    pass

class ClassGroupResponse(ClassGroupBase):
    id: str
    teacher_id: str
    created_at: datetime
    updated_at: Optional[datetime] = None
    learners: List[LearnerResponse] = []

    class Config:
        from_attributes = True
