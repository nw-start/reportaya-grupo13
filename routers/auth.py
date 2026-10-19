from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy import select
from sqlalchemy.orm import Session

from database import get_database
from models import User
from schemas import AuthResponse, LoginRequest, UserResponse, UserRole
from security import create_access_token, decode_access_token, verify_password


router = APIRouter(prefix="/api/auth", tags=["Autenticacion"])
bearer = HTTPBearer(auto_error=False)


def authentication_error(detail: str):
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=detail,
        headers={"WWW-Authenticate": "Bearer"},
    )


def current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    database: Session = Depends(get_database),
) -> User:
    if credentials is None:
        raise authentication_error("Debe iniciar sesion.")

    try:
        payload = decode_access_token(credentials.credentials)
        user_id = int(payload["sub"])
    except (InvalidTokenError, KeyError, TypeError, ValueError, RuntimeError):
        raise authentication_error("Token invalido o expirado.") from None

    user = database.get(User, user_id)
    if user is None or not user.is_active:
        raise authentication_error("Usuario inactivo o inexistente.")

    return user


def administrator_user(user: User = Depends(current_user)) -> User:
    if user.role != UserRole.ADMINISTRATOR.value:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Esta operacion requiere un usuario administrador.",
        )
    return user


@router.post("/login", response_model=AuthResponse)
def login(
    login_data: LoginRequest,
    database: Session = Depends(get_database),
):
    user = database.scalar(select(User).where(User.email == login_data.email))

    if (
        user is None
        or not user.is_active
        or not verify_password(login_data.password, user.password_hash)
    ):
        raise authentication_error("Correo o contrasena incorrectos.")

    return AuthResponse(
        access_token=create_access_token(user.id, user.role),
        user=UserResponse.model_validate(user),
    )


@router.get("/me", response_model=UserResponse)
def get_current_user(user: User = Depends(current_user)):
    return user
