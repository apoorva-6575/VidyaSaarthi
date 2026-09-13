from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List, Optional
from pydantic import BaseModel
import uuid
from datetime import datetime

from app.api.dependencies import get_db, get_current_teacher
from app.models.material_request import MaterialRequest
from app.models.teacher import Teacher
from app.models.class_group import ClassGroup

router = APIRouter()

class MaterialRequestCreate(BaseModel):
    class_id: str
    package_id: str
    requester_id: str

class MaterialRequestUpdate(BaseModel):
    status: str
    provider_id: Optional[str] = None

class MaterialRequestResponse(BaseModel):
    id: str
    class_id: str
    package_id: str
    requester_id: str
    provider_id: Optional[str]
    status: str
    created_at: datetime
    updated_at: Optional[datetime]

    class Config:
        from_attributes = True

@router.post("/", response_model=MaterialRequestResponse)
def create_request(req: MaterialRequestCreate, db: Session = Depends(get_db)):
    req_id = str(uuid.uuid4())
    db_req = MaterialRequest(
        id=req_id,
        class_id=req.class_id,
        package_id=req.package_id,
        requester_id=req.requester_id,
        status="PENDING"
    )
    db.add(db_req)
    db.commit()
    db.refresh(db_req)
    return db_req

@router.get("/", response_model=List[MaterialRequestResponse])
def get_requests(class_id: Optional[str] = None, db: Session = Depends(get_db)):
    query = db.query(MaterialRequest)
    if class_id:
        query = query.filter(MaterialRequest.class_id == class_id)
    return query.all()

@router.patch("/{request_id}", response_model=MaterialRequestResponse)
def update_request(request_id: str, update_data: MaterialRequestUpdate, db: Session = Depends(get_db)):
    db_req = db.query(MaterialRequest).filter(MaterialRequest.id == request_id).first()
    if not db_req:
        raise HTTPException(status_code=404, detail="Request not found")
        
    db_req.status = update_data.status
    if update_data.provider_id:
        db_req.provider_id = update_data.provider_id
        
    db.commit()
    db.refresh(db_req)
    return db_req
