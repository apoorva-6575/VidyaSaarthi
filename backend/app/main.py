from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import auth, learners, classes, sync, content, teacher, material_requests

app = FastAPI(
    title="Rural EdTech Platform - Group 4 Backend",
    description="Backend for offline-first educational platform",
    version="1.0.0"
)

from app.core.config import settings

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.BACKEND_CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

@app.on_event("startup")
def startup_event():
    from app.db.base_class import Base
    import app.models  # noqa: F401
    from app.db.session import engine
    Base.metadata.create_all(bind=engine)
    from app.core.minio_client import ensure_buckets
    ensure_buckets()

@app.get("/health")
def health_check():
    status = {"status": "ok", "database": "ok", "storage": "ok"}
    try:
        from app.db.session import SessionLocal
        from sqlalchemy import text
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
    except Exception as e:
        status["database"] = f"error: {str(e)}"
        status["status"] = "error"
        
    try:
        import socket
        from app.core.config import settings
        host, _, port = settings.MINIO_ENDPOINT.partition(":")
        port_num = int(port) if port else 9000
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(0.5)
        result = sock.connect_ex((host, port_num))
        sock.close()
        if result != 0:
            status["storage"] = "offline/local-mode"
        else:
            from app.core.minio_client import minio_client, CONTENT_BUCKET
            minio_client.bucket_exists(CONTENT_BUCKET)
    except Exception as e:
        status["storage"] = f"error: {str(e)}"
        
    return status

app.include_router(auth.router, prefix="/api/v1/auth", tags=["Auth"])
app.include_router(sync.router, prefix="/api/v1/sync", tags=["Sync"])
app.include_router(content.router, prefix="/api/v1/content", tags=["Content"])
app.include_router(teacher.router, prefix="/api/v1/teacher", tags=["Teacher Analytics"])
app.include_router(learners.router, prefix="/api/v1/learners", tags=["Learners"])
app.include_router(classes.router, prefix="/api/v1/classes", tags=["Classes"])
app.include_router(material_requests.router, prefix="/api/v1/material-requests", tags=["Material Requests"])
