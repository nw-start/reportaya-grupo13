from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database import get_database
from models import User
from schemas import UserCreate, UserResponse
from security import hash_password


router = APIRouter(prefix="/api/usuarios", tags=["Usuarios"])

@router.post("", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def create_user(
    user_data: UserCreate,
    database: Session = Depends(get_database),
):
    user = User(
        name=user_data.name,
        email=user_data.email,
        password_hash=hash_password(user_data.password),
        role=user_data.role.value,
    )

    try:
        database.add(user)
        database.commit()
        database.refresh(user)
    except IntegrityError as error:
        database.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Ya existe un usuario con ese correo.",
        ) from error

    return user


@router.get("", response_model=list[UserResponse])
def list_users(database: Session = Depends(get_database)):
    query = (
        select(User)
        .where(User.is_active.is_(True))
        .order_by(User.name)
    )
    return database.scalars(query).all()
