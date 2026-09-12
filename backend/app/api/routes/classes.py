from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import uuid

from app.api.dependencies import get_db, get_current_teacher
from app.models.class_group import ClassGroup
from app.models.learner import Learner, learner_class_association
from app.schemas.learner import ClassGroupResponse, ClassGroupCreate, JoinClassRequest
from app.models.teacher import Teacher

router = APIRouter()

@router.post("/join", response_model=ClassGroupResponse)
def join_class(join_req: JoinClassRequest, db: Session = Depends(get_db)):
    clean_code = join_req.code.strip().lower()
    if not clean_code:
        raise HTTPException(status_code=400, detail="Class code is required")
        
    all_classes = db.query(ClassGroup).all()
    matched_class = None
    for c in all_classes:
        if c.id.lower().startswith(clean_code) or c.id.lower() == clean_code:
            matched_class = c
            break
            
    if not matched_class:
        raise HTTPException(status_code=404, detail="No class found matching that code")
        
    db_learner = db.query(Learner).filter(Learner.id == join_req.learner_id).first()
    if not db_learner:
        db_learner = Learner(
            id=join_req.learner_id,
            name=join_req.name or f"Learner {join_req.learner_id[:4]}",
            grade=join_req.grade or matched_class.grade,
            preferred_language=join_req.preferred_language or "en"
        )
        db.add(db_learner)
        db.commit()
        db.refresh(db_learner)
        
    if db_learner not in matched_class.learners:
        matched_class.learners.append(db_learner)
        db.commit()
        db.refresh(matched_class)
        
    return matched_class

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
