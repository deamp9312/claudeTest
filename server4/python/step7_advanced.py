"""
[단계 7] 파이썬 고급 기능 - 데코레이터, 제너레이터, 타입 힌트, dataclass

핵심 개념:
  - 데코레이터(Decorator): 함수/클래스를 감싸 동작을 추가 (AOP와 유사)
  - 제너레이터(Generator): 값을 하나씩 지연 생성 (메모리 효율)
  - 타입 힌트(Type Hint): 코드 가독성과 IDE 지원 향상
  - dataclass: 보일러플레이트를 줄인 데이터 클래스 (Java의 Lombok @Data 유사)
"""

import logging
import time
import functools
from typing import Generator, Iterator, Any
from dataclasses import dataclass, field

log = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────────────
# 7-1. 데코레이터(Decorator)
#   - 함수를 인자로 받아 새로운 함수를 반환하는 고차 함수
#   - @데코레이터 문법으로 간편하게 적용
#   - Java의 AOP(Aspect Oriented Programming)와 유사한 개념
# ──────────────────────────────────────────────────────────────────────────────

# 기본 데코레이터: 실행 시간 측정
def timer(func):
    @functools.wraps(func)  # 원본 함수의 메타데이터(__name__, __doc__) 보존
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)   # 원본 함수 실행
        elapsed = time.perf_counter() - start
        log.info("[timer] %s() → %.4f초", func.__name__, elapsed)
        return result
    return wrapper


# 인자를 받는 데코레이터 (팩토리 패턴)
def retry(max_attempts: int = 3, delay: float = 0.1):
    """실패 시 재시도 데코레이터"""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_attempts:
                        log.info("[retry] %s 최대 재시도 초과: %s", func.__name__, e)
                        raise
                    log.info("[retry] %s 시도 %d 실패, 재시도...", func.__name__, attempt)
                    time.sleep(delay)
        return wrapper
    return decorator


# 클래스 데코레이터
def singleton(cls):
    """싱글톤 데코레이터 - 인스턴스 하나만 생성"""
    instances = {}

    @functools.wraps(cls)
    def get_instance(*args, **kwargs):
        if cls not in instances:
            instances[cls] = cls(*args, **kwargs)
        return instances[cls]

    return get_instance


@singleton
class Config:
    def __init__(self):
        self.debug = False
        self.version = "1.0"


def demo_decorator():
    log.info("── [7-1] 데코레이터 ──")

    @timer
    def slow_task():
        time.sleep(0.01)
        return "완료"

    result = slow_task()
    log.info("  결과: %s", result)

    attempt_count = 0

    @retry(max_attempts=3, delay=0.01)
    def flaky_function():
        nonlocal attempt_count
        attempt_count += 1
        if attempt_count < 3:
            raise ValueError(f"일시적 오류 (시도 {attempt_count})")
        return "성공!"

    result = flaky_function()
    log.info("  retry 결과: %s (시도횟수=%d)", result, attempt_count)

    # 싱글톤
    c1 = Config()
    c2 = Config()
    log.info("  singleton: c1 is c2 → %s", c1 is c2)  # True

    # 여러 데코레이터 중첩 (아래에서 위로 적용)
    @timer
    @retry(max_attempts=2, delay=0.01)
    def combined():
        return "중첩 데코레이터"

    log.info("  중첩 결과: %s", combined())


# ──────────────────────────────────────────────────────────────────────────────
# 7-2. 제너레이터(Generator)
#   - yield 키워드를 사용해 값을 하나씩 반환
#   - 전체 결과를 메모리에 올리지 않고 필요할 때 하나씩 생성
#   - 무한 수열, 대용량 파일 처리 등에 유용
# ──────────────────────────────────────────────────────────────────────────────
def count_up(start: int = 0) -> Generator[int, None, None]:
    """무한 카운터 제너레이터"""
    current = start
    while True:
        yield current   # yield: 값을 반환하고 일시 중단 (다음 next() 호출까지)
        current += 1


def fibonacci() -> Generator[int, None, None]:
    """피보나치 수열 제너레이터"""
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b


def read_large_file(filepath: str) -> Iterator[str]:
    """대용량 파일을 한 줄씩 읽는 제너레이터 (메모리 효율)"""
    # 실제 파일 없어도 패턴 설명용
    lines = ["line 1\n", "line 2\n", "line 3\n"]  # 가상 데이터
    for line in lines:
        yield line.strip()


def demo_generator():
    log.info("── [7-2] 제너레이터 ──")

    # 무한 카운터에서 첫 5개만 취하기
    counter = count_up(1)
    first5 = [next(counter) for _ in range(5)]
    log.info("  count_up 첫 5개: %s", first5)

    # itertools.islice: 제너레이터에서 n개 추출
    import itertools
    fib = fibonacci()
    first10_fib = list(itertools.islice(fib, 10))
    log.info("  피보나치 10개: %s", first10_fib)

    # 대용량 파일 처리 패턴
    for line in read_large_file("data.txt"):
        log.info("  파일 라인: %s", line)

    # 제너레이터 표현식 vs 리스트 컴프리헨션
    # 리스트: 모든 값을 즉시 메모리에 생성
    list_result = [x ** 2 for x in range(1000)]      # 1000개 즉시 생성

    # 제너레이터: 필요할 때 하나씩 생성 (메모리 절약)
    gen_result = (x ** 2 for x in range(1000))        # 아직 계산 안 함
    log.info("  제너레이터 합계: %d", sum(gen_result))  # 필요할 때 계산

    # send()로 제너레이터에 값 전달 (양방향 통신)
    def accumulator():
        total = 0
        while True:
            value = yield total   # yield가 값을 받으면서 동시에 반환
            if value is None:
                break
            total += value

    acc = accumulator()
    next(acc)       # 제너레이터 시작 (첫 yield까지 실행)
    acc.send(10)    # 10 전달
    acc.send(20)    # 20 전달
    result = acc.send(30)   # 30 전달 후 현재 total 반환
    log.info("  누적합(10+20+30): %d", result)


# ──────────────────────────────────────────────────────────────────────────────
# 7-3. 타입 힌트(Type Hint) - Python 3.5+
#   - 코드 가독성 향상, mypy/IDE로 정적 분석 가능
#   - 런타임에는 강제되지 않음 (힌트일 뿐)
# ──────────────────────────────────────────────────────────────────────────────
from typing import Union, Optional, TypeVar, Generic

T = TypeVar('T')  # 제네릭 타입 변수 (Java의 <T>)


def first_element(lst: list[T]) -> Optional[T]:
    """제네릭: 리스트의 첫 요소 반환, 비어있으면 None"""
    return lst[0] if lst else None


def process(value: Union[int, str]) -> str:
    """Union: 여러 타입 허용 (Python 3.10+에서는 int | str 으로 가능)"""
    return str(value)


class Stack(Generic[T]):
    """제네릭 클래스"""

    def __init__(self) -> None:
        self._items: list[T] = []

    def push(self, item: T) -> None:
        self._items.append(item)

    def pop(self) -> T:
        if not self._items:
            raise IndexError("스택이 비어있습니다")
        return self._items.pop()

    def __len__(self) -> int:
        return len(self._items)


# ──────────────────────────────────────────────────────────────────────────────
# 7-4. dataclass - 보일러플레이트 제거
#   - @dataclass 데코레이터로 __init__, __repr__, __eq__ 자동 생성
#   - Java의 Lombok @Data / Kotlin data class와 유사
# ──────────────────────────────────────────────────────────────────────────────
@dataclass
class Point:
    x: float
    y: float

    def distance_to(self, other: "Point") -> float:
        return ((self.x - other.x) ** 2 + (self.y - other.y) ** 2) ** 0.5


@dataclass
class User:
    name: str
    age: int
    email: str = ""                         # 기본값 있는 필드
    tags: list[str] = field(default_factory=list)  # 가변 기본값은 field() 사용

    def __post_init__(self):
        # __init__ 이후 추가 초기화
        if self.age < 0:
            raise ValueError(f"나이는 0 이상: {self.age}")


@dataclass(frozen=True)  # 불변 dataclass (tuple처럼 해시 가능)
class ImmutablePoint:
    x: float
    y: float


def demo_type_hints_and_dataclass():
    log.info("── [7-3,4] 타입힌트 & dataclass ──")

    # 타입 힌트
    log.info("  first_element([1,2,3])=%s", first_element([1, 2, 3]))
    log.info("  first_element([])=%s", first_element([]))

    stack: Stack[int] = Stack()
    stack.push(1)
    stack.push(2)
    stack.push(3)
    log.info("  Stack pop: %d, %d", stack.pop(), stack.pop())
    log.info("  Stack len: %d", len(stack))

    # dataclass
    p1 = Point(0.0, 0.0)
    p2 = Point(3.0, 4.0)
    log.info("  Point: %s", p1)                    # __repr__ 자동 생성
    log.info("  거리: %.1f", p1.distance_to(p2))   # 유클리드 거리 = 5.0
    log.info("  p1 == Point(0,0): %s", p1 == Point(0.0, 0.0))  # __eq__ 자동 생성

    u = User("홍길동", 25, tags=["admin", "user"])
    log.info("  User: %s", u)

    ip = ImmutablePoint(1.0, 2.0)
    log.info("  ImmutablePoint: %s (hash=%d)", ip, hash(ip))  # frozen이라 해시 가능


def run() -> str:
    log.info("=== [단계7] 고급 기능 시작 ===")
    demo_decorator()
    demo_generator()
    demo_type_hints_and_dataclass()
    log.info("=== [단계7] 완료 ===")

    return (
        "[단계7 완료] 파이썬 고급 기능\n"
        "- 데코레이터: def deco(func): ... return wrapper / @functools.wraps 필수\n"
        "- 인자 데코레이터: def deco(arg): def decorator(func): ... return decorator\n"
        "- 제너레이터: def gen(): yield value / next()로 하나씩 소비\n"
        "- 타입 힌트: def f(x: int) -> str / Optional[T] / Union[A,B] / Generic[T]\n"
        "- @dataclass: __init__/__repr__/__eq__ 자동 생성, frozen=True로 불변\n"
        "- dataclass field(): 가변 기본값(list/dict) 설정 시 필수\n"
    )
