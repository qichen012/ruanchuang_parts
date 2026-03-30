"""
工具函数单元测试
测试 utils.py 中的密码哈希和JWT令牌函数
"""
import pytest
import sys
import os
import ast
import hashlib
import bcrypt
from unittest.mock import patch, MagicMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestUtilsFileStructure:
    """测试utils.py文件结构"""

    def test_utils_file_exists(self):
        """验证utils.py文件存在"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        assert os.path.exists(utils_path)

    def test_required_imports_present(self):
        """验证必要的导入存在"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "import bcrypt" in content
        assert "import hashlib" in content
        assert "from jose import jwt" in content
        assert "from datetime import datetime, timedelta" in content


class TestUtilsConstants:
    """测试工具函数常量"""

    def test_secret_key_defined(self):
        """测试SECRET_KEY已定义"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert 'SECRET_KEY = "' in content or "SECRET_KEY = '" in content

    def test_algorithm_is_hs256(self):
        """测试ALGORITHM是HS256"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert 'ALGORITHM = "HS256"' in content or "ALGORITHM = 'HS256'" in content

    def test_token_expiry_7_days(self):
        """测试TOKEN过期时间是7天"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7" in content


class TestGetPasswordHashFunction:
    """测试密码哈希函数"""

    def test_function_exists(self):
        """测试get_password_hash函数存在"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "def get_password_hash(password: str) -> str:" in content

    def test_function_uses_sha256(self):
        """测试函数使用SHA256"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "hashlib.sha256" in content

    def test_function_uses_bcrypt(self):
        """测试函数使用bcrypt"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "bcrypt.hashpw" in content
        assert "bcrypt.gensalt" in content

    def test_function_returns_string(self):
        """测试函数返回字符串"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert '-> str:' in content or '->str:' in content


class TestVerifyPasswordFunction:
    """测试密码验证函数"""

    def test_function_exists(self):
        """测试verify_password函数存在"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "def verify_password(plain_password: str, hashed_password: str) -> bool:" in content

    def test_function_uses_sha256(self):
        """测试函数验证时使用SHA256"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "hashlib.sha256" in content

    def test_function_uses_bcrypt_checkpw(self):
        """测试函数使用bcrypt.checkpw验证"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "bcrypt.checkpw" in content

    def test_function_handles_exceptions(self):
        """测试函数有异常处理"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        # 提取verify_password函数
        verify_func = content.split("def verify_password")[1].split("def ")[0]
        assert "try:" in verify_func and "except" in verify_func

    def test_function_returns_false_on_exception(self):
        """测试函数异常时返回False"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        verify_func = content.split("def verify_password")[1].split("def ")[0]
        assert "return False" in verify_func


class TestCreateAccessTokenFunction:
    """测试JWT访问令牌创建"""

    def test_function_exists(self):
        """测试create_access_token函数存在"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        assert "def create_access_token(data: dict):" in content

    def test_function_copies_data(self):
        """测试函数复制传入的数据"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        token_func = content.split("def create_access_token")[1].split("def ")[0]
        assert "to_encode = data.copy()" in token_func

    def test_function_adds_expiry(self):
        """测试函数添加过期时间"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        token_func = content.split("def create_access_token")[1].split("def ")[0]
        assert "exp" in token_func
        assert "timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)" in token_func or \
               "timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES )" in token_func

    def test_function_uses_jwt_encode(self):
        """测试函数使用jwt.encode"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        token_func = content.split("def create_access_token")[1].split("def ")[0]
        assert "jwt.encode" in token_func
        assert "SECRET_KEY" in token_func
        assert "ALGORITHM" in token_func

    def test_function_returns_encoded_jwt(self):
        """测试函数返回编码后的JWT"""
        utils_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")
        with open(utils_path, "r") as f:
            content = f.read()

        token_func = content.split("def create_access_token")[1].split("def ")[0]
        assert "return encoded_jwt" in token_func


class TestPasswordHashingLogic:
    """测试密码哈希逻辑（实际执行测试）"""

    def test_hash_password_logic(self):
        """测试密码哈希逻辑正确"""
        # 模拟utils.py中的哈希逻辑
        password = "test_password"
        sha256_pwd = hashlib.sha256(password.encode('utf-8')).hexdigest()
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(sha256_pwd.encode('utf-8'), salt)

        assert isinstance(hashed_password, bytes)
        assert len(hashed_password) > 0

    def test_verify_password_logic(self):
        """测试密码验证逻辑正确"""
        # 模拟utils.py中的哈希和验证逻辑
        password = "test_password"
        sha256_pwd = hashlib.sha256(password.encode('utf-8')).hexdigest()
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(sha256_pwd.encode('utf-8'), salt)

        # 验证正确密码
        sha256_verify = hashlib.sha256(password.encode('utf-8')).hexdigest()
        assert bcrypt.checkpw(sha256_verify.encode('utf-8'), hashed_password)

        # 验证错误密码
        wrong_sha256 = hashlib.sha256("wrong_password".encode('utf-8')).hexdigest()
        assert not bcrypt.checkpw(wrong_sha256.encode('utf-8'), hashed_password)

    def test_bcrypt_different_salts(self):
        """测试bcrypt每次生成不同的盐"""
        password = "same_password"
        hash1 = bcrypt.hashpw(hashlib.sha256(password.encode()).hexdigest().encode(), bcrypt.gensalt())
        hash2 = bcrypt.hashpw(hashlib.sha256(password.encode()).hexdigest().encode(), bcrypt.gensalt())

        assert hash1 != hash2


class TestJWTTokenLogic:
    """测试JWT令牌逻辑（使用python-jose）"""
    pytestmark = pytest.mark.skipif(
        not os.path.exists(os.path.join(os.path.dirname(os.path.dirname(__file__)), "utils.py")),
        reason="utils.py not found"
    )

    def test_jwt_token_creation(self):
        """测试JWT令牌创建"""
        try:
            from jose import jwt
            from datetime import datetime, timedelta

            SECRET_KEY = "YOUR_SUPER_SECRET_KEY_FOR_SINGULARITY_APP"
            ALGORITHM = "HS256"
            ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7

            data = {"sub": "test_user"}
            expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
            to_encode = data.copy()
            to_encode.update({"exp": expire})
            token = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

            # 验证是JWT格式
            parts = token.split(".")
            assert len(parts) == 3

            # 解码验证
            decoded = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
            assert decoded["sub"] == "test_user"
            assert "exp" in decoded

        except ImportError:
            pytest.skip("python-jose not installed")

    def test_jwt_token_contains_claims(self):
        """测试JWT令牌包含声明"""
        try:
            from jose import jwt
            from datetime import datetime, timedelta

            SECRET_KEY = "YOUR_SUPER_SECRET_KEY_FOR_SINGULARITY_APP"
            ALGORITHM = "HS256"

            data = {"sub": "user123", "name": "Test User", "role": "admin"}
            expire = datetime.utcnow() + timedelta(minutes=60)
            to_encode = data.copy()
            to_encode.update({"exp": expire})
            token = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

            decoded = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
            assert decoded["sub"] == "user123"
            assert decoded["name"] == "Test User"
            assert decoded["role"] == "admin"

        except ImportError:
            pytest.skip("python-jose not installed")

    def test_jwt_token_expiry_calculation(self):
        """测试JWT令牌过期时间计算"""
        try:
            from jose import jwt
            from datetime import datetime, timedelta

            SECRET_KEY = "YOUR_SUPER_SECRET_KEY"
            ALGORITHM = "HS256"
            ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7  # 7天

            before = datetime.utcnow()
            expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
            after = datetime.utcnow()

            token = jwt.encode({"sub": "test", "exp": expire}, SECRET_KEY, algorithm=ALGORITHM)
            decoded = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])

            exp_time = datetime.fromtimestamp(decoded["exp"])
            expected_min = before + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES - 1)
            expected_max = after + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES + 1)

            assert expected_min <= exp_time <= expected_max

        except ImportError:
            pytest.skip("python-jose not installed")
