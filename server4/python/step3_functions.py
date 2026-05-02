"""
[단계 3] 함수 - def, 인자, 반환값, 람다, 클로저

핵심 개념:
  - 파이썬 함수는 일급 객체(First-class citizen): 변수에 담고, 인자로 넘기고, 반환 가능
  - 기본값 인자, 키워드 인자, 가변 인자(*args, **kwargs) 지원
  - 클로저(Closure): 함수가 자신을 감싸는 외부 스코프의 변수를 기억
"""

import logging
from typing import Callable

log = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────────────
# 3-1. 기본 함수 정의
#   - def 함수명(매개변수) → 반환타입:  (타입 힌트는 선택 사항)
#   - return 없으면 None 반환
# ──────────────────────────────────────────────────────────────────────────────
def add(a: int, b: int) -> int:
    """두 수를 더해 반환 (docstring: Java의 Javadoc과 유사)"""
    return a + b


def greet(name: str) -> None:
    """반환값 없는 함수 (None 반환)"""
    log.info("[3-1] 안녕하세요, %s!", name)


# ──────────────────────────────────────────────────────────────────────────────
# 3-2. 기본값 인자(Default Argument)
#   - 인자에 기본값 지정 가능 (Java 5 이전에는 오버로딩으로 처리해야 했음)
#   - 주의: 기본값으로 가변 객체(list, dict) 사용 금지 → None으로 받고 내부에서 생성
# ──────────────────────────────────────────────────────────────────────────────
def connect(host: str, port: int = 8080, timeout: int = 30) -> str:
    return f"연결: {host}:{port} (timeout={timeout}s)"


# 기본값 인자 함정: 리스트를 기본값으로 쓰면 모든 호출이 같은 리스트를 공유!
def append_bad(item, lst=[]):   # 잘못된 예 - 모든 호출이 같은 lst를 공유
    lst.append(item)
    return lst


def append_good(item, lst=None):  # 올바른 예 - None 체크 후 새 리스트 생성
    if lst is None:
        lst = []
    lst.append(item)
    return lst


# ──────────────────────────────────────────────────────────────────────────────
# 3-3. 키워드 인자(Keyword Argument)
#   - 인자 이름을 명시해서 순서 무관하게 전달
# ──────────────────────────────────────────────────────────────────────────────
def create_user(name: str, age: int, email: str) -> dict:
    return {"name": name, "age": age, "email": email}


# ──────────────────────────────────────────────────────────────────────────────
# 3-4. 가변 인자 *args / **kwargs
#   - *args  : 위치 인자를 튜플로 받음 (개수 제한 없음)
#   - **kwargs: 키워드 인자를 딕셔너리로 받음
# ──────────────────────────────────────────────────────────────────────────────
def sum_all(*args: int) -> int:
    # args는 튜플: (1, 2, 3) 처럼 전달됨
    log.info("[3-4] args = %s", args)
    return sum(args)  # sum(): 내장 함수로 합계 계산


def print_info(**kwargs) -> None:
    # kwargs는 딕셔너리: {"key": "value"} 형태
    log.info("[3-4] kwargs = %s", kwargs)
    for key, value in kwargs.items():
        log.info("  %s: %s", key, value)


def mixed(required: str, *args, **kwargs) -> None:
    # 순서: 일반인자 → *args → **kwargs
    log.info("[3-4] required=%s, args=%s, kwargs=%s", required, args, kwargs)


# ──────────────────────────────────────────────────────────────────────────────
# 3-5. 다중 반환값
#   - 파이썬은 여러 값을 튜플로 반환 가능 (Java는 객체/배열 필요)
# ──────────────────────────────────────────────────────────────────────────────
def divide(a: int, b: int) -> tuple[int, int]:
    quotient = a // b   # 몫
    remainder = a % b   # 나머지
    return quotient, remainder  # 튜플로 반환


# ──────────────────────────────────────────────────────────────────────────────
# 3-6. 람다 함수(Lambda)
#   - lambda 매개변수: 표현식
#   - 단일 표현식만 가능 (여러 줄 불가)
#   - 주로 sorted(), map(), filter()의 key 인자에 활용
# ──────────────────────────────────────────────────────────────────────────────
def demo_lambda():
    # 기본 람다
    square = lambda x: x ** 2       # x를 받아 x²를 반환
    add_lambda = lambda a, b: a + b

    log.info("[3-6] 람다: square(5)=%d, add(3,4)=%d", square(5), add_lambda(3, 4))

    # sorted()에서 key 함수로 활용
    students = [("Alice", 85), ("Bob", 92), ("Charlie", 78)]
    # 점수(두 번째 요소, [1])로 내림차순 정렬
    sorted_by_score = sorted(students, key=lambda s: s[1], reverse=True)
    log.info("[3-6] 정렬 결과: %s", sorted_by_score)

    # map(): 각 요소에 함수 적용 → Java의 stream().map()과 유사
    numbers = [1, 2, 3, 4, 5]
    squared = list(map(lambda x: x ** 2, numbers))
    log.info("[3-6] map(square): %s → %s", numbers, squared)

    # filter(): 조건 참인 요소만 추출 → Java의 stream().filter()와 유사
    evens = list(filter(lambda x: x % 2 == 0, numbers))
    log.info("[3-6] filter(even): %s → %s", numbers, evens)


# ──────────────────────────────────────────────────────────────────────────────
# 3-7. 클로저(Closure)
#   - 내부 함수가 외부 함수의 변수를 "기억"하는 함수
#   - 상태를 가진 함수를 만들 때 사용 (클래스보다 가볍게)
# ──────────────────────────────────────────────────────────────────────────────
def make_counter(start: int = 0) -> Callable[[], int]:
    """카운터 클로저 생성"""
    count = start  # 외부 함수의 지역 변수 (클로저가 기억할 변수)

    def counter() -> int:
        nonlocal count      # nonlocal: 외부 함수 변수를 수정할 때 필요
        count += 1          # 클로저가 count를 기억하고 유지
        return count

    return counter  # 함수 자체를 반환 (호출하지 않음!)


def make_multiplier(factor: int) -> Callable[[int], int]:
    """곱셈 팩토리 클로저"""
    def multiply(x: int) -> int:
        return x * factor  # factor를 기억

    return multiply


# ──────────────────────────────────────────────────────────────────────────────
# 3-8. 함수를 인자로 받는 고차 함수(Higher-Order Function)
# ──────────────────────────────────────────────────────────────────────────────
def apply_twice(func: Callable[[int], int], value: int) -> int:
    """함수를 두 번 적용"""
    return func(func(value))


def run() -> str:
    log.info("=== [단계3] 함수 시작 ===")

    # 3-1 기본 함수
    log.info("[3-1] add(3, 5) = %d", add(3, 5))
    greet("파이썬")

    # 3-2 기본값 인자
    log.info("[3-2] %s", connect("localhost"))            # port=8080, timeout=30 기본값
    log.info("[3-2] %s", connect("example.com", 443))    # timeout만 기본값
    log.info("[3-2] %s", connect("db.local", 5432, 60))  # 모두 지정

    # 3-3 키워드 인자 (순서 무관)
    user = create_user(age=25, email="test@test.com", name="홍길동")
    log.info("[3-3] 키워드 인자: %s", user)

    # 3-4 가변 인자
    log.info("[3-4] sum_all(1,2,3,4,5) = %d", sum_all(1, 2, 3, 4, 5))
    print_info(name="홍길동", age=25, city="서울")
    mixed("필수값", 1, 2, 3, key1="a", key2="b")

    # 3-5 다중 반환
    q, r = divide(17, 5)
    log.info("[3-5] 17 ÷ 5 = 몫:%d, 나머지:%d", q, r)

    # 3-6 람다
    demo_lambda()

    # 3-7 클로저
    counter1 = make_counter()       # 0부터 시작하는 카운터
    counter2 = make_counter(100)    # 100부터 시작하는 카운터
    log.info("[3-7] counter1: %d, %d, %d", counter1(), counter1(), counter1())  # 1, 2, 3
    log.info("[3-7] counter2: %d, %d", counter2(), counter2())                  # 101, 102

    double = make_multiplier(2)
    triple = make_multiplier(3)
    log.info("[3-7] double(5)=%d, triple(5)=%d", double(5), triple(5))

    # 3-8 고차 함수
    log.info("[3-8] apply_twice(double, 3) = %d", apply_twice(double, 3))  # 3 → 6 → 12

    log.info("=== [단계3] 완료 ===")

    return (
        "[단계3 완료] 함수\n"
        "- def 함수명(인자: 타입) -> 반환타입: (타입 힌트 권장)\n"
        "- 기본값 인자: def f(x, port=8080) — 가변 객체 기본값 금지\n"
        "- 키워드 인자: f(port=443, host='a') — 순서 무관\n"
        "- *args: 가변 위치 인자 (튜플), **kwargs: 가변 키워드 인자 (딕셔너리)\n"
        "- 다중 반환: return a, b → 튜플 반환, a, b = func()으로 언패킹\n"
        "- 람다: lambda x: x*2 — map/filter/sorted의 key에 주로 사용\n"
        "- 클로저: 외부 함수 변수를 기억하는 내부 함수, nonlocal로 수정\n"
    )
