from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from typing import List, Optional
import json

from app.api.dependencies import get_db, get_current_teacher
from app.models.content import ContentPackage
from app.schemas.content import ContentPackageResponse, ContentPackageManifest
from app.models.teacher import Teacher
from app.core.minio_client import minio_client, CONTENT_BUCKET

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

    import hashlib
    
    # Calculate SHA-256 of the uploaded file
    sha256_hash = hashlib.sha256()
    file.file.seek(0)
    for chunk in iter(lambda: file.file.read(4096), b""):
        sha256_hash.update(chunk)
    actual_checksum = sha256_hash.hexdigest()
    
    if actual_checksum != checksum:
        raise HTTPException(status_code=400, detail="Checksum mismatch. Uploaded file corrupted or checksum incorrect.")

    file.file.seek(0, 2)
    file_size = file.file.tell()
    file.file.seek(0)

    object_name = f"{package_id}/{version}/content.zip"
    try:
        minio_client.put_object(
            CONTENT_BUCKET,
            object_name,
            file.file,
            length=file_size,
            content_type=file.content_type
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to upload to storage: {str(e)}")

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

@router.get("/{package_id}/{version}/download")
def download_package(
    package_id: str,
    version: int,
    db: Session = Depends(get_db)
):
    pkg = db.query(ContentPackage).filter(
        ContentPackage.id == package_id,
        ContentPackage.version == version
    ).first()
    if not pkg:
        raise HTTPException(status_code=404, detail="Package not found")
        
    object_name = f"{package_id}/{version}/content.zip"
    try:
        response = minio_client.get_object(CONTENT_BUCKET, object_name)
        return StreamingResponse(
            response.stream(32 * 1024), 
            media_type="application/zip",
            headers={
                "Content-Disposition": f"attachment; filename=pkg_{package_id}_{version}.zip"
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch from storage: {str(e)}")

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
