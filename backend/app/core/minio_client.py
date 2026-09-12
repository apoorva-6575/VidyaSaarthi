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
        import socket
        host, _, port = settings.MINIO_ENDPOINT.partition(":")
        port_num = int(port) if port else 9000
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(0.5)
        result = sock.connect_ex((host, port_num))
        sock.close()
        if result != 0:
            logger.info("MinIO not running; skipping bucket initialization.")
            return

        if not minio_client.bucket_exists(CONTENT_BUCKET):
            minio_client.make_bucket(CONTENT_BUCKET)
            logger.info(f"Created MinIO bucket: {CONTENT_BUCKET}")
    except Exception as e:
        logger.warning(f"MinIO bucket check skipped: {e}")
