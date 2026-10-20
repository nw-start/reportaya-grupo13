from fastapi import FastAPI
from sqlalchemy import select

from database import Base, SessionLocal, engine
from models import IncidentType
from routers.auth import router as auth_router
from routers.incident_types import router as incident_types_router
from routers.incidents import router as incidents_router
from routers.users import router as users_router


Base.metadata.create_all(bind=engine)


def seed_incident_types():
    initial_types = [
        "Fuga de agua",
        "Ascensor",
        "Ruido",
        "Seguridad",
        "Limpieza",
        "Otro",
    ]

    with SessionLocal.begin() as database:
        existing_types = set(database.scalars(select(IncidentType.name)).all())
        for name in initial_types:
            if name not in existing_types:
                database.add(IncidentType(name=name))


seed_incident_types()


app = FastAPI(
    title="ReportaYa API",
    description="Servicio para registrar y gestionar incidencias.",
    version="0.1.0",
)

app.include_router(auth_router)
app.include_router(incidents_router)
app.include_router(incident_types_router)
app.include_router(users_router)


@app.get("/")
def get_home():
    return {"message": "API de ReportaYa funcionando"}
