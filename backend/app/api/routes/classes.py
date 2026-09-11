from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import uuid

from app.api.dependencies import get_db, get_current_teacher
from app.models.class_group import ClassGroup
from app.models.learner import Learner, learner_class_association
from app.schemas.learner import ClassGroupResponse, ClassGroupCreate
from app.models.teacher import Teacher

router = APIRouter()

@router.get("/", response_model=List[ClassGroupResponse])
def get_classes(skip: int = 0, limit: int = 100, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    classes = db.query(ClassGroup).filter(ClassGroup.teacher_id == current_teacher.id).offset(skip).limit(limit).all()
    return classes

@router.post("/", response_model=ClassGroupResponse)
def create_class(class_in: ClassGroupCreate, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    class_id = str(uuid.uuid4())
    db_class = ClassGroup(  
        id=class_id,
        name=class_in.name,
        grade=class_in.grade,
        subject=class_in.subject,
        teacher_id=current_teacher.id
    )
    db.add(db_class)
    db.commit()
    db.refresh(db_class)
    return db_class

@router.post("/{class_id}/learners/{learner_id}")
def add_learner_to_class(class_id: str, learner_id: str, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    db_class = db.query(ClassGroup).filter(ClassGroup.id == class_id, ClassGroup.teacher_id == current_teacher.id).first()
    if not db_class:
        raise HTTPException(status_code=404, detail="Class not found")
        
    db_learner = db.query(Learner).filter(Learner.id == learner_id).first()
    if not db_learner:
        raise HTTPException(status_code=404, detail="Learner not found")
        
    if db_learner not in db_class.learners:
        db_class.learners.append(db_learner)
        db.commit()
    return {"status": "success", "message": "Learner added to class"}
