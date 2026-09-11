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

@router.get("/classes/{class_id}/analytics")
def get_class_analytics(class_id: str, db: Session = Depends(get_db), current_teacher: Teacher = Depends(get_current_teacher)):
    # Verify class ownership
    db_class = db.query(ClassGroup).filter(ClassGroup.id == class_id, ClassGroup.teacher_id == current_teacher.id).first()
    if not db_class:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Class not found or access denied")
        
    learner_ids = [learner.id for learner in db_class.learners]
    if not learner_ids:
        return {
            "class_id": class_id,
            "learner_count": 0,
            "concept_averages": {},
            "learners": []
        }
        
    # Get all mastery and progress events for learners in this class
    events = db.query(LearningEvent).filter(
        LearningEvent.learner_id.in_(learner_ids),
        LearningEvent.event_type.in_(["MASTERY_UPDATED", "PROGRESS_UPDATED"])
    ).all()
    
    mastery_dict = {}
    progress_dict = {}
    
    for event in events:
        lid = event.learner_id
        if event.event_type == "MASTERY_UPDATED":
            cid = event.payload.get("concept_id")
            score = event.payload.get("mastery", 0.0)
            if lid not in mastery_dict:
                mastery_dict[lid] = {}
            if cid not in mastery_dict[lid] or event.server_sequence > mastery_dict[lid][cid]['seq']:
                mastery_dict[lid][cid] = {'score': score, 'seq': event.server_sequence}
        elif event.event_type == "PROGRESS_UPDATED":
            pid = event.payload.get("content_id")
            progress = event.payload.get("progress", 0.0)
            if lid not in progress_dict:
                progress_dict[lid] = {}
            if pid not in progress_dict[lid] or event.server_sequence > progress_dict[lid][pid]['seq']:
                progress_dict[lid][pid] = {'score': progress, 'seq': event.server_sequence}
                
    # Aggregate class-wide concept averages
    concept_totals = {}
    concept_counts = {}
    for lid, concepts in mastery_dict.items():
        for cid, data in concepts.items():
            if cid not in concept_totals:
                concept_totals[cid] = 0.0
                concept_counts[cid] = 0
            concept_totals[cid] += data['score']
            concept_counts[cid] += 1
            
    concept_averages = {
        cid: (concept_totals[cid] / concept_counts[cid])
        for cid in concept_totals
    }
    
    # Build learner level details
    learners_data = []
    for learner in db_class.learners:
        lid = learner.id
        l_mastery = mastery_dict.get(lid, {})
        l_progress = progress_dict.get(lid, {})
        
        # Calculate overall mastery and progress for the learner
        avg_mastery = sum([d['score'] for d in l_mastery.values()]) / len(l_mastery) if l_mastery else 0.0
        avg_progress = sum([d['score'] for d in l_progress.values()]) / len(l_progress) if l_progress else 0.0
        
        # Needs attention logic: low mastery (< 50%) or specific weak concepts
        weak_concepts = [cid for cid, d in l_mastery.items() if d['score'] < 0.5]
        needs_attention = avg_mastery < 0.5 or len(weak_concepts) > 0
        
        learners_data.append({
            "learner_id": lid,
            "learner_name": learner.name,
            "progress": avg_progress,
            "mastery": avg_mastery,
            "weak_concepts": weak_concepts,
            "needs_attention": needs_attention
        })
        
    return {
        "class_id": class_id,
        "learner_count": len(learner_ids),
        "concept_averages": concept_averages,
        "learners": learners_data
    }
