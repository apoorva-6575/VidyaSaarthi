from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import uuid
import random
import string

from app.api.dependencies import get_db, get_current_teacher
from app.models.class_group import ClassGroup
from app.models.learner import Learner, learner_class_association
from app.schemas.learner import ClassGroupResponse, ClassGroupCreate, JoinClassRequest
from app.models.teacher import Teacher
from app.models.class_material import ClassMaterialAssignment
from pydantic import BaseModel

router = APIRouter()

@router.post("/join", response_model=ClassGroupResponse)
def join_class(join_req: JoinClassRequest, db: Session = Depends(get_db)):
    clean_code = join_req.code.strip().upper()
    if not clean_code:
        raise HTTPException(status_code=400, detail="Class code is required")
        
    matched_class = db.query(ClassGroup).filter(ClassGroup.join_code == clean_code).first()
            
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
    
    # Generate unique 6-character join code
    while True:
        join_code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
        if not db.query(ClassGroup).filter(ClassGroup.join_code == join_code).first():
            break
            
    db_class = ClassGroup(  
        id=class_id,
        join_code=join_code,
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

class ClassMaterialRequest(BaseModel):
    package_id: str
    version: int

@router.post("/{class_id}/materials")
def assign_material_to_class(class_id: str, req: ClassMaterialRequest, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    db_class = db.query(ClassGroup).filter(ClassGroup.id == class_id, ClassGroup.teacher_id == current_teacher.id).first()
    if not db_class:
        raise HTTPException(status_code=404, detail="Class not found or unauthorized")
        
    existing = db.query(ClassMaterialAssignment).filter(
        ClassMaterialAssignment.class_id == class_id,
        ClassMaterialAssignment.package_id == req.package_id,
        ClassMaterialAssignment.version == req.version
    ).first()
    
    if existing:
        return {"status": "success", "message": "Material already assigned to class"}
        
    assignment_id = str(uuid.uuid4())
    new_assignment = ClassMaterialAssignment(
        id=assignment_id,
        class_id=class_id,
        package_id=req.package_id,
        version=req.version,
        teacher_id=current_teacher.id
    )
    db.add(new_assignment)
    db.commit()
    return {"status": "success", "message": "Material assigned to class"}

@router.get("/{class_id}/materials")
def get_class_materials(class_id: str, db: Session = Depends(get_db)):
    assignments = db.query(ClassMaterialAssignment).filter(ClassMaterialAssignment.class_id == class_id).all()
    return [{"package_id": a.package_id, "version": a.version, "shared_at": a.created_at} for a in assignments]

