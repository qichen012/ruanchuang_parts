import bcrypt
import hashlib
from jose import jwt
from datetime import datetime, timedelta

SECRET_KEY = "YOUR_SUPER_SECRET_KEY_FOR_SINGULARITY_APP"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7  # 7天过期

def get_password_hash(password: str) -> str:
    sha256_pwd = hashlib.sha256(password.encode('utf-8')).hexdigest()
    salt = bcrypt.gensalt()
    hashed_password = bcrypt.hashpw(sha256_pwd.encode('utf-8'), salt)
    return hashed_password.decode('utf-8')

def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        sha256_pwd = hashlib.sha256(plain_password.encode('utf-8')).hexdigest()
        return bcrypt.checkpw(sha256_pwd.encode('utf-8'), hashed_password.encode('utf-8'))
    except Exception:
        return False

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt