from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from typing import List

from app.api.dependencies import get_db
from app.models.learning_event import LearningEvent
from app.models.sync_state import SyncState
from app.schemas.sync import SyncRequest, SyncResponse, SyncEventPayload

router = APIRouter()

@router.post("/events", response_model=SyncResponse)
def sync_events(sync_req: SyncRequest, db: Session = Depends(get_db)):
    accepted = 0
    duplicate = 0
    
    # 1. Process incoming events
    for event_data in sync_req.events:
        existing_event = db.query(LearningEvent).filter(LearningEvent.id == event_data.event_id).first()
        if existing_event:
            duplicate += 1
            continue
            
        new_event = LearningEvent(  
            id=event_data.event_id,
            learner_id=event_data.learner_id,
            device_id=event_data.device_id,
            event_type=event_data.event_type,
            timestamp=event_data.timestamp,
            payload=event_data.payload,
            schema_version=event_data.schema_version
        )
        db.add(new_event)
        try:
            db.commit()
            accepted += 1
        except IntegrityError:
            db.rollback()
            duplicate += 1
            
    # 2. Update or create sync state for this device
    sync_state = db.query(SyncState).filter(SyncState.device_id == sync_req.device_id).first()
    if not sync_state:
        sync_state = SyncState(device_id=sync_req.device_id)
        db.add(sync_state)
        db.commit()
        db.refresh(sync_state)
        
    # 3. Get events the device doesn't have yet (pull sync)
    new_server_events = db.query(LearningEvent).filter(
        LearningEvent.server_sequence > sync_req.last_server_sequence
    ).order_by(LearningEvent.server_sequence.asc()).all()
    
    # Update device's known last server sequence (if we are giving them new events)
    next_seq = sync_req.last_server_sequence
    if new_server_events:
        next_seq = max(e.server_sequence for e in new_server_events)
        sync_state.last_server_sequence = next_seq
        db.commit()
        
    response_events = [
        SyncEventPayload(
            event_id=e.id,
            learner_id=e.learner_id,
            device_id=e.device_id,
            event_type=e.event_type,
            timestamp=e.timestamp,
            payload=e.payload,
            schema_version=e.schema_version
        ) for e in new_server_events
    ]
    
    return SyncResponse(
        accepted_events=accepted,
        duplicate_events=duplicate,
        next_server_sequence=next_seq,
        new_server_events=response_events
    )
