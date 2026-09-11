from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from app.api.dependencies import get_db, get_current_teacher
from app.models.learner import Learner
from app.schemas.learner import LearnerResponse, LearnerCreate
from app.models.teacher import Teacher

router = APIRouter()

@router.get("/", response_model=List[LearnerResponse])
def get_learners(skip: int = 0, limit: int = 100, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    # In a full app, filter by teacher's classes
    learners = db.query(Learner).offset(skip).limit(limit).all()
    return learners

@router.get("/{learner_id}", response_model=LearnerResponse)
def get_learner(learner_id: str, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    learner = db.query(Learner).filter(Learner.id == learner_id).first()
    if not learner:
        raise HTTPException(status_code=404, detail="Learner not found")
    return learner
