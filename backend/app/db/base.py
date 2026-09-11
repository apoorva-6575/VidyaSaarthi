# Import all the models, so that Base has them before being imported by Alembic
from app.db.base_class import Base  # noqa
from app.models.teacher import Teacher  # noqa
from app.models.learner import Learner, learner_class_association  # noqa
from app.models.class_group import ClassGroup  # noqa
from app.models.learning_event import LearningEvent  # noqa
from app.models.content import ContentPackage  # noqa
from app.models.sync_state import SyncState  # noqa
