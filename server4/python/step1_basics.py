"""
[단계 1] 파이썬 기본 문법 - 변수, 자료형, 연산자

핵심 개념:
  - 파이썬은 동적 타이핑(Dynamic Typing): 변수 선언 시 타입을 명시하지 않음
  - 들여쓰기(Indentation)로 코드 블록을 구분 (Java의 {} 대신)
  - 모든 것이 객체(Object): 숫자, 문자열, 함수 모두 객체
"""

import logging

log = logging.getLogger(__name__)


def run() -> str:
    """단계1 전체 실행 - 기본 문법 데모"""
    log.info("=== [단계1] 파이썬 기본 문법 시작 ===")

    # ──────────────────────────────────────────────────────────────────────────
    # 1-1. 변수 선언과 자료형
    #   - 파이썬은 변수에 타입 선언 없이 바로 할당
    #   - 변수 이름은 snake_case 관례 (Java는 camelCase)
    # ──────────────────────────────────────────────────────────────────────────

    # 정수(int): 크기 제한 없음 (Java의 int/long 구분 불필요)
    age = 30
    big_number = 10 ** 100  # 파이썬은 임의 정밀도 정수 지원

    # 실수(float): IEEE 754 배정밀도 (Java의 double과 동일)
    price = 3.14
    scientific = 1.5e-3  # 1.5 × 10^-3 = 0.0015

    # 문자열(str): 작은따옴표 또는 큰따옴표 모두 사용 가능
    name = "파이썬"
    greeting = '안녕하세요'

    # 불리언(bool): True/False (대문자 시작 - Java와 동일)
    is_active = True
    is_empty = False

    # None: 값이 없음을 표현 (Java의 null과 유사하지만 객체)
    result = None

    log.info("[1-1] 변수 선언: age=%d, price=%.2f, name=%s, is_active=%s, result=%s",
             age, price, name, is_active, result)

    # ──────────────────────────────────────────────────────────────────────────
    # 1-2. type() - 자료형 확인
    #   - 파이썬은 런타임에 타입을 확인할 수 있음
    # ──────────────────────────────────────────────────────────────────────────
    log.info("[1-2] 타입 확인: type(age)=%s, type(price)=%s, type(name)=%s",
             type(age).__name__,    # 'int'
             type(price).__name__,  # 'float'
             type(name).__name__)   # 'str'

    # isinstance(): 타입 체크 권장 방법 (상속 관계도 고려)
    log.info("[1-2] isinstance: %s", isinstance(age, int))  # True

    # ──────────────────────────────────────────────────────────────────────────
    # 1-3. 타입 변환(Type Casting)
    # ──────────────────────────────────────────────────────────────────────────
    num_str = "42"
    num_int = int(num_str)       # 문자열 → 정수
    num_float = float(num_str)   # 문자열 → 실수
    back_to_str = str(num_int)   # 정수 → 문자열

    log.info("[1-3] 타입 변환: '%s' → int=%d, float=%.1f, str='%s'",
             num_str, num_int, num_float, back_to_str)

    # ──────────────────────────────────────────────────────────────────────────
    # 1-4. 연산자
    # ──────────────────────────────────────────────────────────────────────────
    a, b = 10, 3  # 다중 할당 (파이썬의 편의 문법)

    # 산술 연산자
    log.info("[1-4] 산술: %d+%d=%d, %d-%d=%d, %d*%d=%d",
             a, b, a + b,
             a, b, a - b,
             a, b, a * b)
    log.info("[1-4] 나눗셈: %d/%d=%.1f (실수), %d//%d=%d (정수몫), %d%%%d=%d (나머지), %d**%d=%d (거듭제곱)",
             a, b, a / b,     # / 는 항상 float 반환 (Java와 다름!)
             a, b, a // b,    # // 정수 나눗셈 (몫)
             a, b, a % b,     # % 나머지
             a, b, a ** b)    # ** 거듭제곱 (Java의 Math.pow 대신)

    # 비교 연산자 (결과는 bool)
    log.info("[1-4] 비교: %d>%d=%s, %d==%d=%s, %d!=%d=%s",
             a, b, a > b,
             a, b, a == b,
             a, b, a != b)

    # 논리 연산자: and / or / not (Java의 && / || / ! 대신 영어 단어)
    log.info("[1-4] 논리: True and False=%s, True or False=%s, not True=%s",
             True and False,
             True or False,
             not True)

    # ──────────────────────────────────────────────────────────────────────────
    # 1-5. 문자열 활용
    # ──────────────────────────────────────────────────────────────────────────
    s = "Hello, Python!"

    # 인덱싱: 0부터 시작, 음수 인덱스는 뒤에서부터
    log.info("[1-5] 인덱싱: s[0]=%s, s[-1]=%s", s[0], s[-1])

    # 슬라이싱: s[start:end:step] - end는 미포함
    log.info("[1-5] 슬라이싱: s[0:5]=%s, s[7:]=%s, s[::-1]=%s",
             s[0:5],    # 'Hello'
             s[7:],     # 'Python!'
             s[::-1])   # 문자열 뒤집기

    # f-string (Python 3.6+): 가장 권장하는 문자열 포매팅
    # Java의 String.format() 또는 문자열 + 연결보다 가독성 좋음
    user = "홍길동"
    score = 95.5
    formatted = f"학생: {user}, 점수: {score:.1f}점"  # .1f = 소수점 1자리
    log.info("[1-5] f-string: %s", formatted)

    # 문자열 메서드
    log.info("[1-5] 메서드: upper=%s, lower=%s, strip=%s, split=%s",
             "hello".upper(),           # 'HELLO'
             "WORLD".lower(),           # 'world'
             "  spaces  ".strip(),      # 'spaces' (앞뒤 공백 제거)
             "a,b,c".split(","))        # ['a', 'b', 'c']

    # ──────────────────────────────────────────────────────────────────────────
    # 1-6. 다중 할당과 변수 교환
    #   - 파이썬의 우아한 문법 (Java는 temp 변수 필요)
    # ──────────────────────────────────────────────────────────────────────────
    x, y = 1, 2
    log.info("[1-6] 교환 전: x=%d, y=%d", x, y)
    x, y = y, x  # 임시 변수 없이 교환! (파이썬 튜플 언패킹)
    log.info("[1-6] 교환 후: x=%d, y=%d", x, y)

    log.info("=== [단계1] 완료 ===")

    return (
        "[단계1 완료] 파이썬 기본 문법\n"
        "- 변수: 타입 선언 없이 바로 할당 (동적 타이핑)\n"
        "- 자료형: int, float, str, bool, None\n"
        "- /  → 실수 나눗셈, // → 정수 나눗셈, ** → 거듭제곱\n"
        "- 논리연산: and / or / not (영어 키워드)\n"
        "- f-string: f'{변수:.형식}' 으로 문자열 포매팅\n"
        "- 다중 할당: a, b = 1, 2 / 변수 교환: a, b = b, a\n"
    )
