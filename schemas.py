from datetime import date, datetime, time
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, field_validator


class IncidentStatus(str, Enum):
    PENDING = "Pendiente"
    IN_PROGRESS = "En proceso"
    ATTENDED = "Atendido"
    REJECTED = "Rechazado"


class UserRole(str, Enum):
    RESIDENT = "Residente"
    ADMINISTRATOR = "Administrador"


class UserCreate(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        str_strip_whitespace=True,
        extra="forbid",
    )

    name: str = Field(alias="nombres", min_length=2, max_length=100)
    email: str = Field(alias="correo", min_length=5, max_length=120)
    password: str = Field(alias="contrasena", min_length=8, max_length=72)
    role: UserRole = Field(default=UserRole.RESIDENT, alias="rol")

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str):
        if "@" not in value or "." not in value.rsplit("@", maxsplit=1)[-1]:
            raise ValueError("debe ingresar un correo valido")
        return value.lower()


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int = Field(serialization_alias="idUsuario")
    name: str = Field(serialization_alias="nombres")
    email: str = Field(serialization_alias="correo")
    role: UserRole = Field(serialization_alias="rol")
    is_active: bool = Field(serialization_alias="activo")
    created_at: datetime = Field(serialization_alias="fechaCreacion")


class UserSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int = Field(serialization_alias="idUsuario")
    name: str = Field(serialization_alias="nombres")


class IncidentTypeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int = Field(serialization_alias="idTipoIncidencia")
    name: str = Field(serialization_alias="nombre")
    is_active: bool = Field(serialization_alias="activo")


class IncidentBase(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        str_strip_whitespace=True,
        extra="forbid",
    )

    registration_date: date = Field(alias="fechaRegistro")
    registration_time: time = Field(alias="horaRegistro")
    description: str = Field(alias="descripcion", min_length=10, max_length=250)
    location: str = Field(alias="ubicacion", min_length=1, max_length=60)
    photo_url: str | None = Field(default=None, alias="fotoUrl", max_length=500)

    @field_validator("registration_date")
    @classmethod
    def validate_registration_date(cls, value: date):
        if value > date.today():
            raise ValueError("la fecha de registro no puede ser futura")
        return value


class IncidentCreate(IncidentBase):
    incident_type_id: int = Field(alias="idTipoIncidencia", gt=0)
    registered_by_id: int = Field(alias="idUsuarioRegistro", gt=0)


class IncidentUpdate(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        str_strip_whitespace=True,
        extra="forbid",
    )

    incident_type_id: int | None = Field(
        default=None,
        alias="idTipoIncidencia",
        gt=0,
    )
    description: str | None = Field(
        default=None,
        alias="descripcion",
        min_length=10,
        max_length=250,
    )
    location: str | None = Field(
        default=None,
        alias="ubicacion",
        min_length=1,
        max_length=60,
    )
    status: IncidentStatus | None = Field(default=None, alias="estado")
    attention_note: str | None = Field(
        default=None,
        alias="observacionAtencion",
        min_length=1,
        max_length=250,
    )


class IncidentResponse(IncidentBase):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int = Field(serialization_alias="idIncidencia")
    code: str = Field(serialization_alias="codigo")
    status: IncidentStatus = Field(serialization_alias="estado")
    attended_at: datetime | None = Field(
        default=None,
        serialization_alias="fechaAtencion",
    )
    attention_note: str | None = Field(
        default=None,
        serialization_alias="observacionAtencion",
    )
    is_deleted: bool = Field(serialization_alias="eliminado")
    updated_at: datetime = Field(serialization_alias="fechaActualizacion")
    incident_type_id: int = Field(serialization_alias="idTipoIncidencia")
    registered_by_id: int = Field(serialization_alias="idUsuarioRegistro")
    incident_type: IncidentTypeResponse = Field(
        serialization_alias="tipoIncidencia"
    )
    registered_by: UserSummary = Field(serialization_alias="usuarioRegistro")
