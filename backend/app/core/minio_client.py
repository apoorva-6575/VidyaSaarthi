from minio import Minio
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

# Initialize MinIO client
minio_client = Minio(
    settings.MINIO_ENDPOINT,
    access_key=settings.MINIO_ACCESS_KEY,
    secret_key=settings.MINIO_SECRET_KEY,
    secure=settings.MINIO_SECURE
)

CONTENT_BUCKET = "edtech-content"

def ensure_buckets():
    try:
        if not minio_client.bucket_exists(CONTENT_BUCKET):
            minio_client.make_bucket(CONTENT_BUCKET)
            logger.info(f"Created MinIO bucket: {CONTENT_BUCKET}")
    except Exception as e:
        logger.error(f"Error ensuring MinIO bucket exists: {e}")
