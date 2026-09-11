from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from typing import List

from app.api.dependencies import get_db, get_current_teacher
from app.models.learner import Learner, learner_class_association
from app.schemas.learner import LearnerResponse, LearnerCreate
from app.models.teacher import Teacher
from app.models.class_group import ClassGroup

router = APIRouter()

@router.get("/", response_model=List[LearnerResponse])
def get_learners(skip: int = 0, limit: int = 100, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    # Filter by teacher's classes
    class_ids = [c.id for c in db.query(ClassGroup.id).filter(ClassGroup.teacher_id == current_teacher.id).all()]
    learners = db.query(Learner).join(learner_class_association).filter(
        learner_class_association.c.class_id.in_(class_ids)
    ).offset(skip).limit(limit).all()
    return learners

@router.get("/{learner_id}", response_model=LearnerResponse)
def get_learner(learner_id: str, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    class_ids = [c.id for c in db.query(ClassGroup.id).filter(ClassGroup.teacher_id == current_teacher.id).all()]
    learner = db.query(Learner).join(learner_class_association).filter(
        Learner.id == learner_id,
        learner_class_association.c.class_id.in_(class_ids)
    ).first()
    if not learner:
        raise HTTPException(status_code=404, detail="Learner not found or access denied")
    return learner

@router.post("/", response_model=LearnerResponse)
def create_learner(learner_in: LearnerCreate, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    existing = db.query(Learner).filter(Learner.id == learner_in.id).first()
    if existing:
        return existing
        
    db_learner = Learner(
        id=learner_in.id,
        name=learner_in.name,
        grade=learner_in.grade,
        preferred_language=learner_in.preferred_language
    )
    db.add(db_learner)
    try:
        db.commit()
        db.refresh(db_learner)
    except IntegrityError:
        db.rollback()
        existing = db.query(Learner).filter(Learner.id == learner_in.id).first()
        if existing:
            return existing
        raise HTTPException(status_code=500, detail="Failed to create learner")
    return db_learner
