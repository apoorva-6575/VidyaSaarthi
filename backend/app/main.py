from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import auth, learners, classes, sync, content, teacher

app = FastAPI(
    title="Rural EdTech Platform - Group 4 Backend",
    description="Backend for offline-first educational platform",
    version="1.0.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

@app.get("/health")
def health_check():
    return {"status": "ok", "database": "ok", "storage": "ok"}

app.include_router(auth.router, prefix="/api/v1/auth", tags=["Auth"])
app.include_router(sync.router, prefix="/api/v1/sync", tags=["Sync"])
app.include_router(content.router, prefix="/api/v1/content", tags=["Content"])
app.include_router(teacher.router, prefix="/api/v1/teacher", tags=["Teacher Analytics"])
app.include_router(learners.router, prefix="/api/v1/learners", tags=["Learners"])
app.include_router(classes.router, prefix="/api/v1/classes", tags=["Classes"])
