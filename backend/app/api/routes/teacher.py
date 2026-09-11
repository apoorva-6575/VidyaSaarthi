from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import List

from app.api.dependencies import get_db, get_current_teacher
from app.models.teacher import Teacher
from app.models.learning_event import LearningEvent
from app.models.class_group import ClassGroup
from app.models.learner import learner_class_association

router = APIRouter()

@router.get("/dashboard")
def get_teacher_dashboard(db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    # Simple dashboard aggregation
    classes = db.query(ClassGroup).filter(ClassGroup.teacher_id == current_teacher.id).all()
    class_ids = [c.id for c in classes]
    
    # Get all learners in these classes
    learner_ids = db.query(learner_class_association.c.learner_id).filter(
        learner_class_association.c.class_id.in_(class_ids)
    ).all()
    learner_ids = [l[0] for l in learner_ids]
    
    # Get mastery events
    mastery_events = db.query(LearningEvent).filter(
        LearningEvent.learner_id.in_(learner_ids),
        LearningEvent.event_type == "MASTERY_UPDATED"
    ).all()
    
    # Process latest mastery per learner per concept
    mastery_dict = {}
    for event in mastery_events:
        lid = event.learner_id
        cid = event.payload.get("concept_id")
        score = event.payload.get("mastery", 0)
        
        if lid not in mastery_dict:
            mastery_dict[lid] = {}
        
        # In a real app we'd sort by timestamp to ensure we keep the latest.
        # Assuming event insertion order correlates with server_sequence which correlates roughly with time
        if cid not in mastery_dict[lid] or event.server_sequence > mastery_dict[lid][cid]['seq']:
             mastery_dict[lid][cid] = {'score': score, 'seq': event.server_sequence}
             
    # Aggregate class wide mastery per concept
    concept_totals = {}
    concept_counts = {}
    
    for lid, concepts in mastery_dict.items():
        for cid, data in concepts.items():
            if cid not in concept_totals:
                concept_totals[cid] = 0
                concept_counts[cid] = 0
            concept_totals[cid] += data['score']
            concept_counts[cid] += 1
            
    concept_averages = {
        cid: (concept_totals[cid] / concept_counts[cid]) 
        for cid in concept_totals
    }
    
    return {
        "classes_count": len(classes),
        "learners_count": len(learner_ids),
        "concept_averages": concept_averages
    }
