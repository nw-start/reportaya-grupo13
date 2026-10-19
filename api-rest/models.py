from datetime import date, datetime, time

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    String,
    Time,
    false,
    func,
    true,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database import Base


class User(Base):
    __tablename__ = "usuario"
    __table_args__ = (
        CheckConstraint(
            "rol IN ('Residente', 'Administrador')",
            name="chk_usuario_rol",
        ),
    )

    id: Mapped[int] = mapped_column("id_usuario", Integer, primary_key=True)
    name: Mapped[str] = mapped_column("nombres", String(100), nullable=False)
    email: Mapped[str] = mapped_column(
        "correo", String(120), unique=True, nullable=False
    )
    password_hash: Mapped[str] = mapped_column(
        "contrasena_hash", String(255), nullable=False
    )
    role: Mapped[str] = mapped_column(
        "rol", String(20), nullable=False, default="Residente"
    )
    is_active: Mapped[bool] = mapped_column(
        "activo", Boolean, nullable=False, default=True, server_default=true()
    )
    created_at: Mapped[datetime] = mapped_column(
        "fecha_creacion", DateTime, nullable=False, server_default=func.now()
    )

    incidents: Mapped[list["Incident"]] = relationship(back_populates="registered_by")


class IncidentType(Base):
    __tablename__ = "tipo_incidencia"

    id: Mapped[int] = mapped_column(
        "id_tipo_incidencia", Integer, primary_key=True
    )
    name: Mapped[str] = mapped_column(
        "nombre", String(30), unique=True, nullable=False
    )
    is_active: Mapped[bool] = mapped_column(
        "activo", Boolean, nullable=False, default=True, server_default=true()
    )

    incidents: Mapped[list["Incident"]] = relationship(back_populates="incident_type")


class Incident(Base):
    __tablename__ = "incidencia"
    __table_args__ = (
        CheckConstraint(
            "length(descripcion) BETWEEN 10 AND 250",
            name="chk_incidencia_descripcion",
        ),
        CheckConstraint(
            "estado IN ('Pendiente', 'En proceso', 'Atendido', 'Rechazado')",
            name="chk_incidencia_estado",
        ),
        Index("idx_incidencia_fecha", "fecha_registro"),
        Index("idx_incidencia_estado", "estado"),
        Index("idx_incidencia_eliminado", "eliminado"),
    )

    id: Mapped[int] = mapped_column(
        "id_incidencia", Integer, primary_key=True, autoincrement=True
    )
    code: Mapped[str] = mapped_column("codigo", String(15), unique=True, nullable=False)
    registration_date: Mapped[date] = mapped_column(
        "fecha_registro", Date, nullable=False
    )
    registration_time: Mapped[time] = mapped_column(
        "hora_registro", Time, nullable=False
    )
    description: Mapped[str] = mapped_column(
        "descripcion", String(250), nullable=False
    )
    location: Mapped[str] = mapped_column("ubicacion", String(60), nullable=False)
    status: Mapped[str] = mapped_column("estado", String(15), nullable=False)
    photo_url: Mapped[str | None] = mapped_column(
        "foto_url", String(500), nullable=True
    )
    attended_at: Mapped[datetime | None] = mapped_column(
        "fecha_atencion", DateTime, nullable=True
    )
    attention_note: Mapped[str | None] = mapped_column(
        "observacion_atencion", String(250), nullable=True
    )
    is_deleted: Mapped[bool] = mapped_column(
        "eliminado", Boolean, nullable=False, default=False, server_default=false()
    )
    updated_at: Mapped[datetime] = mapped_column(
        "fecha_actualizacion",
        DateTime,
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )
    incident_type_id: Mapped[int] = mapped_column(
        "id_tipo_incidencia",
        ForeignKey("tipo_incidencia.id_tipo_incidencia", ondelete="RESTRICT"),
        nullable=False,
    )
    registered_by_id: Mapped[int] = mapped_column(
        "id_usuario_registro",
        ForeignKey("usuario.id_usuario", ondelete="RESTRICT"),
        nullable=False,
    )

    incident_type: Mapped[IncidentType] = relationship(back_populates="incidents")
    registered_by: Mapped[User] = relationship(back_populates="incidents")
