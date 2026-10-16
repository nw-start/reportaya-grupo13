from datetime import datetime
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import or_, select
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session
from sqlalchemy.orm import selectinload

from database import get_database
from models import Incident, IncidentType, User
from schemas import IncidentCreate, IncidentResponse, IncidentStatus, IncidentUpdate


router = APIRouter(prefix="/api/incidencias", tags=["Incidencias"])


@router.post("", response_model=IncidentResponse, status_code=status.HTTP_201_CREATED)
def create_incident(
    incident_data: IncidentCreate,
    database: Session = Depends(get_database),
):
    incident_type = database.get(IncidentType, incident_data.incident_type_id)
    if incident_type is None or not incident_type.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="El tipo de incidencia no es valido.",
        )

    registered_by = database.get(User, incident_data.registered_by_id)
    if registered_by is None or not registered_by.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="El usuario no es valido.",
        )

    incident = Incident(
        code=f"TMP-{uuid4().hex[:11]}",
        registration_date=incident_data.registration_date,
        registration_time=incident_data.registration_time,
        description=incident_data.description,
        location=incident_data.location,
        photo_url=incident_data.photo_url,
        status=IncidentStatus.PENDING.value,
        incident_type_id=incident_type.id,
        registered_by_id=registered_by.id,
    )

    try:
        database.add(incident)
        database.flush()
        incident.code = f"INC-{incident.id:04d}"
        database.commit()
        database.refresh(incident)
    except SQLAlchemyError as error:
        database.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="No se pudo registrar la incidencia.",
        ) from error

    return incident


@router.get("", response_model=list[IncidentResponse])
def list_incidents(database: Session = Depends(get_database)):
    query = (
        select(Incident)
        .options(
            selectinload(Incident.incident_type),
            selectinload(Incident.registered_by),
        )
        .where(Incident.is_deleted.is_(False))
        .order_by(Incident.registration_date.desc(), Incident.id.desc())
    )

    return database.scalars(query).all()


@router.get("/buscar", response_model=list[IncidentResponse])
def search_incidents(
    criterio: str = Query(min_length=1, max_length=250),
    database: Session = Depends(get_database),
):
    search_term = criterio.strip()

    if not search_term:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Debe ingresar un criterio de búsqueda.",
        )

    search_value = f"%{search_term}%"
    query = (
        select(Incident)
        .join(Incident.incident_type)
        .join(Incident.registered_by)
        .options(
            selectinload(Incident.incident_type),
            selectinload(Incident.registered_by),
        )
        .where(
            Incident.is_deleted.is_(False),
            or_(
                Incident.code.ilike(search_value),
                Incident.description.ilike(search_value),
                Incident.location.ilike(search_value),
                Incident.status.ilike(search_value),
                IncidentType.name.ilike(search_value),
                User.name.ilike(search_value),
            ),
        )
        .order_by(Incident.registration_date.desc(), Incident.id.desc())
    )

    return database.scalars(query).all()


@router.get("/{incident_id}", response_model=IncidentResponse)
def get_incident(
    incident_id: int,
    database: Session = Depends(get_database),
):
    query = select(Incident).where(
        Incident.id == incident_id,
        Incident.is_deleted.is_(False),
    ).options(
        selectinload(Incident.incident_type),
        selectinload(Incident.registered_by),
    )
    incident = database.scalar(query)

    if incident is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Incidencia no encontrada.",
        )

    return incident


@router.patch("/{incident_id}", response_model=IncidentResponse)
def update_incident(
    incident_id: int,
    incident_data: IncidentUpdate,
    database: Session = Depends(get_database),
):
    query = select(Incident).where(
        Incident.id == incident_id,
        Incident.is_deleted.is_(False),
    ).options(
        selectinload(Incident.incident_type),
        selectinload(Incident.registered_by),
    )
    incident = database.scalar(query)

    if incident is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Incidencia no encontrada.",
        )

    changes = incident_data.model_dump(exclude_unset=True)
    if not changes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Debe ingresar al menos un campo para actualizar.",
        )

    if "incident_type_id" in changes:
        incident_type = database.get(IncidentType, changes["incident_type_id"])
        if incident_type is None or not incident_type.is_active:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El tipo de incidencia no es valido.",
            )
        incident.incident_type_id = incident_type.id

    for field in ("description", "location", "attention_note"):
        if field in changes:
            setattr(incident, field, changes[field])

    if "status" in changes:
        new_status = changes["status"]
        incident.status = new_status.value
        incident.attended_at = (
            datetime.now() if new_status == IncidentStatus.ATTENDED else None
        )

    try:
        database.commit()
        database.refresh(incident)
    except SQLAlchemyError as error:
        database.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="No se pudo actualizar la incidencia.",
        ) from error

    return incident
