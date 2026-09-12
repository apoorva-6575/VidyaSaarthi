import os
import sys
import uuid
from datetime import datetime, timezone

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from app.db.base_class import Base
from app.db.session import SessionLocal, engine
from app.models.teacher import Teacher
from app.models.learner import Learner
from app.models.class_group import ClassGroup
from app.models.learning_event import LearningEvent
from app.security.authentication import get_password_hash

def seed_db():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    
    # 1. Create a teacher
    teacher_id = str(uuid.uuid4())
    teacher = Teacher(  # type: ignore
        id=teacher_id,
        name="Demo Teacher",
        email="teacher@demo.com",
        hashed_password=get_password_hash("password123")
    )
    db.add(teacher)
    
    # 2. Create a class
    class_id = str(uuid.uuid4())
    class_group = ClassGroup(  # type: ignore
        id=class_id,
        name="Class 6A",
        subject="Mathematics",
        teacher_id=teacher_id
    )
    db.add(class_group)
    
    # 3. Create Learners
    learners_data = [
        {"id": f"L00{i}", "name": f"Learner {i}", "grade": "6"} for i in range(1, 6)
    ]
    learners = []
    for data in learners_data:
        learner = Learner(id=data["id"], name=data["name"], grade=data["grade"])  # type: ignore
        learners.append(learner)
        class_group.learners.append(learner)
        db.add(learner)
        
    # 4. Create Learning Events (Mastery)
    # Give some learners low mastery in Fractions, and high in Geometry
    now = datetime.now(timezone.utc)
    for learner in learners:
        # Fractions event (Needs attention for most)
        score = 0.3 if learner.id in ["L001", "L002", "L003", "L004"] else 0.8
        event_frac = LearningEvent(  # type: ignore
            id=str(uuid.uuid4()),
            learner_id=learner.id,
            device_id="device-sim-1",
            event_type="MASTERY_UPDATED",
            timestamp=now,
            payload={"concept_id": "fractions", "mastery": score}
        )
        db.add(event_frac)
        
        # Geometry event (Strong for most)
        event_geo = LearningEvent(  # type: ignore
            id=str(uuid.uuid4()),
            learner_id=learner.id,
            device_id="device-sim-1",
            event_type="MASTERY_UPDATED",
            timestamp=now,
            payload={"concept_id": "geometry", "mastery": 0.85}
        )
        db.add(event_geo)

    db.commit()
    print("Database seeded successfully! Demo Teacher login: teacher@demo.com / password123")
    
if __name__ == "__main__":
    seed_db()
