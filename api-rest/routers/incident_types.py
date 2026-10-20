from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from database import get_database
from models import IncidentType, User
from routers.auth import current_user
from schemas import IncidentTypeResponse


router = APIRouter(prefix="/api/tipos-incidencia", tags=["Tipos de incidencia"])


@router.get("", response_model=list[IncidentTypeResponse])
def list_incident_types(
    database: Session = Depends(get_database),
    _user: User = Depends(current_user),
):
    query = (
        select(IncidentType)
        .where(IncidentType.is_active.is_(True))
        .order_by(IncidentType.name)
    )
    return database.scalars(query).all()
