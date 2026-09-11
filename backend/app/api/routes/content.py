from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List, Optional
import json

from app.api.dependencies import get_db, get_current_teacher
from app.models.content import ContentPackage
from app.schemas.content import ContentPackageResponse, ContentPackageManifest
from app.models.teacher import Teacher

router = APIRouter()

@router.get("/", response_model=List[ContentPackageResponse])
def get_content_packages(
    skip: int = 0, 
    limit: int = 100, 
    status: Optional[str] = "PUBLISHED",
    db: Session = Depends(get_db)
):
    query = db.query(ContentPackage)
    if status:
        query = query.filter(ContentPackage.status == status)
    return query.offset(skip).limit(limit).all()

@router.post("/", response_model=ContentPackageResponse)
def upload_content_package(
    package_id: str = Form(...),
    version: int = Form(...),
    subject: str = Form(...),
    grade: str = Form(...),
    language: str = Form(...),
    checksum: str = Form(...),
    manifest: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_teacher: Teacher = Depends(get_current_teacher)
):
    # Check if exists
    existing = db.query(ContentPackage).filter(
        ContentPackage.id == package_id, 
        ContentPackage.version == version
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="Package version already exists")
        
    try:
        manifest_data = json.loads(manifest)
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="Invalid JSON in manifest")

    # In a real app, we would upload `file.file` to MinIO here
    # file_size = upload_to_minio(file.file, f"{package_id}_{version}.zip")
    
    file.file.seek(0, 2)
    file_size = file.file.tell()

    new_pkg = ContentPackage(
        id=package_id,
        version=version,
        subject=subject,
        grade=grade,
        language=language,
        checksum=checksum,
        size=file_size,
        status="PUBLISHED", # Auto publish for MVP
        metadata_json=manifest_data
    )
    
    db.add(new_pkg)
    db.commit()
    db.refresh(new_pkg)
    return new_pkg

@router.post("/{package_id}/{version}/revoke")
def revoke_package(
    package_id: str,
    version: int,
    db: Session = Depends(get_db),
    current_teacher: Teacher = Depends(get_current_teacher)
):
    pkg = db.query(ContentPackage).filter(
        ContentPackage.id == package_id,
        ContentPackage.version == version
    ).first()
    if not pkg:
        raise HTTPException(status_code=404, detail="Package not found")
        
    pkg.status = "REVOKED"
    db.commit()
    return {"status": "success", "message": "Package revoked"}
