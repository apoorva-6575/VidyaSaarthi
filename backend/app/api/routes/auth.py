from datetime import timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
import uuid

from app.core.config import settings
from app.db.session import SessionLocal
from app.api.dependencies import get_db, get_current_teacher
from app.models.teacher import Teacher
from app.schemas.auth import TeacherCreate, TeacherResponse, Token
from app.security.authentication import verify_password, get_password_hash, create_access_token

router = APIRouter()

@router.post("/register", response_model=TeacherResponse)
def register_teacher(teacher_in: TeacherCreate, db: Session = Depends(get_db)):
    teacher = db.query(Teacher).filter(Teacher.email == teacher_in.email).first()
    if teacher:
        raise HTTPException(
            status_code=400,
            detail="The teacher with this email already exists in the system.",
        )
    
    teacher_id = str(uuid.uuid4())
    db_teacher = Teacher(  # type: ignore
        id=teacher_id,
        name=teacher_in.name,
        email=teacher_in.email,
        hashed_password=get_password_hash(teacher_in.password),
        organization_id=teacher_in.organization_id
    )
    db.add(db_teacher)
    db.commit()
    db.refresh(db_teacher)
    return db_teacher

@router.post("/login", response_model=Token)
def login_access_token(db: Session = Depends(get_db), form_data: OAuth2PasswordRequestForm = Depends()):
    teacher = db.query(Teacher).filter(Teacher.email == form_data.username).first()
    if not teacher or not verify_password(form_data.password, teacher.hashed_password):
        raise HTTPException(status_code=400, detail="Incorrect email or password")
    
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": teacher.email}, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

@router.get("/me", response_model=TeacherResponse)
def read_teacher_me(current_teacher: Teacher = Depends(get_current_teacher)):
    return current_teacher
