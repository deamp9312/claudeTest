"""
[단계 6] 예외처리 - try/except/else/finally, 커스텀 예외

핵심 개념:
  - 파이썬은 "EAFP(Easier to Ask Forgiveness than Permission)" 스타일 권장
    → 사전 조건 검사보다 예외 처리를 선호
  - Java의 checked exception 개념 없음 (모든 예외가 unchecked)
  - with 문: 자원 관리를 위한 컨텍스트 매니저 (Java의 try-with-resources)
"""

import logging

log = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────────────
# 6-1. 기본 예외 처리 구조
#   try:      → 예외 발생 가능 코드
#   except:   → 예외 처리
#   else:     → 예외 없을 때 실행 (파이썬 고유)
#   finally:  → 항상 실행 (Java와 동일)
# ──────────────────────────────────────────────────────────────────────────────
def demo_basic():
    log.info("── [6-1] 기본 예외처리 ──")

    # 단순 try-except
    try:
        result = 10 / 0
    except ZeroDivisionError:
        log.info("ZeroDivisionError 처리: 0으로 나눌 수 없음")

    # 여러 예외 처리
    def safe_parse(value: str) -> int | None:
        try:
            return int(value)
        except ValueError:
            log.info("ValueError: '%s'는 정수로 변환 불가", value)
            return None
        except TypeError:
            log.info("TypeError: 타입이 맞지 않음")
            return None

    log.info("safe_parse('42')=%s", safe_parse("42"))
    log.info("safe_parse('abc')=%s", safe_parse("abc"))
    log.info("safe_parse(None)=%s", safe_parse(None))  # type: ignore

    # 여러 예외를 하나의 except로 처리
    try:
        data = [1, 2, 3]
        val = data[10]  # IndexError
    except (IndexError, KeyError) as e:
        # as e: 예외 객체를 변수에 바인딩 (Java의 catch (Exception e))
        log.info("IndexError 또는 KeyError: %s", e)

    # except Exception as e: 모든 일반 예외 잡기
    # except BaseException: 시스템 종료(SystemExit) 등 포함 (사용 지양)

    # else: 예외 없이 정상 완료 시 실행
    # finally: 예외 유무와 상관없이 항상 실행
    def read_number(s: str) -> str:
        try:
            num = int(s)
        except ValueError as e:
            return f"실패: {e}"
        else:
            # try 블록이 성공했을 때만 실행 (예외 없음이 확실한 정리 작업)
            log.info("  정상 파싱: %d", num)
            return f"성공: {num}"
        finally:
            # 항상 실행 (DB 연결 닫기, 파일 닫기 등)
            log.info("  finally 실행 (항상)")

    log.info("read_number('123'): %s", read_number("123"))
    log.info("read_number('abc'): %s", read_number("abc"))


# ──────────────────────────────────────────────────────────────────────────────
# 6-2. 예외 발생 (raise)
# ──────────────────────────────────────────────────────────────────────────────
def demo_raise():
    log.info("── [6-2] raise ──")

    def set_age(age: int) -> None:
        if age < 0:
            raise ValueError(f"나이는 0 이상이어야 합니다: {age}")
        if age > 150:
            raise ValueError(f"나이가 너무 큽니다: {age}")
        log.info("나이 설정: %d", age)

    try:
        set_age(-5)
    except ValueError as e:
        log.info("ValueError 처리: %s", e)

    # raise ... from ...: 예외 연쇄 (원인 예외 보존)
    def connect_db(url: str) -> None:
        try:
            raise ConnectionError("연결 실패")  # 가상의 에러
        except ConnectionError as e:
            raise RuntimeError("DB 초기화 실패") from e  # 원인 연결

    try:
        connect_db("jdbc:localhost")
    except RuntimeError as e:
        log.info("RuntimeError: %s (원인: %s)", e, e.__cause__)

    # 예외 재발생
    def process():
        try:
            raise ValueError("처리 중 오류")
        except ValueError:
            log.info("예외 로깅 후 재발생")
            raise  # 동일한 예외를 다시 발생

    try:
        process()
    except ValueError as e:
        log.info("재발생된 예외 처리: %s", e)


# ──────────────────────────────────────────────────────────────────────────────
# 6-3. 커스텀 예외 클래스
#   - Exception을 상속해서 도메인 전용 예외 만들기
#   - Java의 custom exception과 동일한 개념
# ──────────────────────────────────────────────────────────────────────────────

class AppError(Exception):
    """애플리케이션 기본 예외 (모든 커스텀 예외의 부모)"""

    def __init__(self, message: str, code: int = 500):
        super().__init__(message)  # 부모 Exception의 message 설정
        self.code = code
        self.message = message

    def __str__(self) -> str:
        return f"[{self.code}] {self.message}"


class ValidationError(AppError):
    """입력값 검증 실패"""

    def __init__(self, field: str, message: str):
        super().__init__(f"검증 실패 - {field}: {message}", code=400)
        self.field = field


class NotFoundError(AppError):
    """리소스를 찾을 수 없음"""

    def __init__(self, resource: str, id_: int):
        super().__init__(f"{resource}(id={id_})를 찾을 수 없습니다", code=404)


def demo_custom_exception():
    log.info("── [6-3] 커스텀 예외 ──")

    def find_user(user_id: int) -> dict:
        db = {1: {"name": "Alice"}, 2: {"name": "Bob"}}
        if user_id not in db:
            raise NotFoundError("User", user_id)
        return db[user_id]

    def create_user(name: str, age: int) -> dict:
        if not name:
            raise ValidationError("name", "이름은 필수입니다")
        if age < 0:
            raise ValidationError("age", "나이는 0 이상이어야 합니다")
        return {"name": name, "age": age}

    # 커스텀 예외 처리
    for uid in [1, 99]:
        try:
            user = find_user(uid)
            log.info("유저 조회 성공: %s", user)
        except NotFoundError as e:
            log.info("Not Found: %s (code=%d)", e, e.code)

    try:
        create_user("", 25)
    except ValidationError as e:
        log.info("Validation: %s (field=%s)", e, e.field)

    # 부모 예외로도 잡을 수 있음 (다형성)
    try:
        find_user(999)
    except AppError as e:
        log.info("AppError (부모로 처리): code=%d", e.code)


# ──────────────────────────────────────────────────────────────────────────────
# 6-4. 컨텍스트 매니저 (with 문)
#   - __enter__ / __exit__ 매직 메서드로 구현
#   - Java의 try-with-resources와 동일한 목적 (자원 자동 해제)
# ──────────────────────────────────────────────────────────────────────────────
class DatabaseConnection:
    """가상 DB 연결 - 컨텍스트 매니저 구현"""

    def __init__(self, host: str):
        self.host = host
        self.connected = False

    def __enter__(self):
        # with 블록 진입 시 실행 (Java의 try 블록 전 초기화)
        log.info("  DB 연결: %s", self.host)
        self.connected = True
        return self  # as 변수에 바인딩될 값

    def __exit__(self, exc_type, exc_val, exc_tb):
        # with 블록 종료 시 실행 (예외 발생해도 반드시 실행)
        # exc_type: 예외 타입 (없으면 None)
        # 반환값 True → 예외 억제, False/None → 예외 전파
        self.connected = False
        log.info("  DB 연결 해제: %s", self.host)
        if exc_type:
            log.info("  예외 발생: %s", exc_val)
        return False  # 예외 전파

    def query(self, sql: str) -> list:
        if not self.connected:
            raise RuntimeError("연결되지 않음")
        log.info("  쿼리 실행: %s", sql)
        return [{"id": 1, "name": "Alice"}]


def demo_context_manager():
    log.info("── [6-4] 컨텍스트 매니저 ──")

    # with 문: __enter__ → 블록 실행 → __exit__ (항상)
    with DatabaseConnection("localhost:5432") as db:
        results = db.query("SELECT * FROM users")
        log.info("  결과: %s", results)
    # with 블록 종료 후 자동으로 연결 해제

    # contextlib.contextmanager: 제너레이터로 컨텍스트 매니저 만들기
    from contextlib import contextmanager

    @contextmanager
    def timer(label: str):
        import time
        start = time.time()
        log.info("  [%s] 시작", label)
        try:
            yield  # 여기서 with 블록 실행
        finally:
            elapsed = time.time() - start
            log.info("  [%s] 완료: %.3f초", label, elapsed)

    with timer("작업"):
        import time
        time.sleep(0.01)  # 10ms 대기

    # 파일은 내장 컨텍스트 매니저 제공
    # with open("file.txt", "r") as f:
    #     content = f.read()
    # → 블록 종료 시 자동으로 f.close() 호출


def run() -> str:
    log.info("=== [단계6] 예외처리 시작 ===")
    demo_basic()
    demo_raise()
    demo_custom_exception()
    demo_context_manager()
    log.info("=== [단계6] 완료 ===")

    return (
        "[단계6 완료] 예외처리\n"
        "- try/except/else/finally: else는 예외 없을 때만 실행\n"
        "- except (A, B) as e: 여러 예외를 하나로 처리\n"
        "- raise ValueError('메시지'): 예외 발생\n"
        "- raise RuntimeError('') from e: 예외 연쇄 (원인 보존)\n"
        "- class MyError(Exception): 커스텀 예외 (Exception 상속)\n"
        "- with 컨텍스트매니저 as x: 자원 자동 해제 (try-with-resources)\n"
        "- @contextmanager + yield: 제너레이터로 간단한 컨텍스트 매니저 구현\n"
    )
