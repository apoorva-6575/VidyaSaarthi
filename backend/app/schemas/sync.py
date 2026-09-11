from pydantic import BaseModel, Field
from typing import List, Dict, Any
from datetime import datetime

class SyncEventPayload(BaseModel):
    event_id: str
    learner_id: str
    device_id: str
    event_type: str
    timestamp: datetime
    payload: Dict[str, Any]
    schema_version: int = 1

class SyncRequest(BaseModel):
    device_id: str
    last_server_sequence: int
    events: List[SyncEventPayload] = []

class SyncResponse(BaseModel):
    accepted_events: int
    duplicate_events: int
    next_server_sequence: int
    new_server_events: List[SyncEventPayload] = []
