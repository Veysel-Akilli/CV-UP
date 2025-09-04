from starlette.middleware.base import BaseHTTPMiddleware
from fastapi import Request, HTTPException, status
from fastapi.security import HTTPBearer
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.security import verify_token
from app.services.user_service import UserService
from typing import Optional

import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("jwt")

security = HTTPBearer()

class JWTMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        logger.info(f"🟡 Middleware başladı. URL: {request.url.path}")

        public_paths = [
    "/docs",
    "/redoc",
    "/openapi.json",
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/document-requests/generate",
    "/health"
]

        if any(request.url.path.startswith(path) for path in public_paths):
            logger.info("🟢 Public endpoint'e istek geldi. Doğrulama atlandı.")
            return await call_next(request)

        auth_header = request.headers.get("Authorization")
        logger.info(f"🔐 Authorization header: {auth_header}")

        if not auth_header:
            logger.warning("❌ Authorization header eksik")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authorization header missing",
                headers={"WWW-Authenticate": "Bearer"},
            )

        try:
            scheme, token = auth_header.split()
            logger.info(f"🔑 Token scheme: {scheme}, token başlangıcı: {token[:15]}...")

            if scheme.lower() != "bearer":
                logger.warning("❌ Hatalı authentication scheme")
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid authentication scheme",
                    headers={"WWW-Authenticate": "Bearer"},
                )
            
            logger.info("📤 Token çözümleme başlatıldı...")
            payload = verify_token(token)
            logger.info(f"📦 Token çözümlendi: {payload}")

            if payload is None:
                logger.warning("❌ Geçersiz veya süresi geçmiş token")
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid token",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            email = payload.get("sub")
            if email is None:
                logger.warning("❌ Token içinde 'sub' (email) alanı yok")
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid token payload",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            logger.info(f"✅ Doğrulanan kullanıcı e-postası: {email}")
            request.state.user_email = email

        except ValueError:
            logger.error("❌ Authorization header formatı hatalı (2 parça bekleniyor)")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authorization header format",
                headers={"WWW-Authenticate": "Bearer"},
            )

        return await call_next(request)

def get_current_user(request: Request) -> Optional[str]:
    return getattr(request.state, 'user_email', None)

def require_auth(request: Request):
    user_email = get_current_user(request)
    if not user_email:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return user_email
