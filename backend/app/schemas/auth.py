from pydantic import BaseModel, EmailStr
from typing import Optional

class TeacherCreate(BaseModel):
    name: str
    email: EmailStr
    password: str
    organization_id: Optional[str] = None

class TeacherResponse(BaseModel):
    id: str
    name: str
    email: EmailStr
    organization_id: Optional[str] = None

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    email: Optional[str] = None
