"""
[단계 2] 제어문 - if / for / while

핵심 개념:
  - 파이썬은 중괄호 {} 없이 들여쓰기(4칸)로 블록을 구분
  - for문이 Java보다 훨씬 강력: 모든 iterable을 순회 가능
  - else 절을 for/while에도 사용 가능 (파이썬만의 특징)
"""

import logging

log = logging.getLogger(__name__)


def run() -> str:
    log.info("=== [단계2] 제어문 시작 ===")

    # ──────────────────────────────────────────────────────────────────────────
    # 2-1. if / elif / else
    #   - Java의 else if → 파이썬은 elif
    #   - 조건식에 괄호 불필요 (관례상 생략)
    # ──────────────────────────────────────────────────────────────────────────
    score = 85

    if score >= 90:
        grade = "A"
    elif score >= 80:   # Java의 else if
        grade = "B"
    elif score >= 70:
        grade = "C"
    else:
        grade = "F"

    log.info("[2-1] 점수=%d → 등급=%s", score, grade)

    # 조건부 표현식 (삼항 연산자): 값 if 조건 else 값2
    # Java: condition ? value1 : value2
    status = "합격" if score >= 60 else "불합격"
    log.info("[2-1] 삼항연산자: %s", status)

    # in 연산자: 리스트/문자열 포함 여부 확인 (Java의 contains)
    fruits = ["사과", "바나나", "딸기"]
    log.info("[2-1] 'in' 연산자: '바나나' in fruits → %s", "바나나" in fruits)

    # ──────────────────────────────────────────────────────────────────────────
    # 2-2. for 반복문
    #   - for 변수 in 순회가능한것(iterable):
    #   - Java의 for-each와 유사하지만 더 범용적
    # ──────────────────────────────────────────────────────────────────────────

    # 리스트 순회
    log.info("[2-2] 리스트 순회:")
    for fruit in fruits:
        log.info("  - %s", fruit)

    # range(): 숫자 범위 생성 (start, stop, step)
    # range(5)      → 0, 1, 2, 3, 4
    # range(1, 6)   → 1, 2, 3, 4, 5
    # range(0, 10, 2) → 0, 2, 4, 6, 8
    log.info("[2-2] range(1, 6): %s", list(range(1, 6)))
    log.info("[2-2] range(0, 10, 2): %s", list(range(0, 10, 2)))

    # enumerate(): 인덱스 + 값 동시에 얻기
    # Java: for (int i = 0; i < list.size(); i++) 와 동일
    log.info("[2-2] enumerate:")
    for index, fruit in enumerate(fruits, start=1):  # start=1 이면 1부터 카운트
        log.info("  %d번째: %s", index, fruit)

    # zip(): 여러 리스트 동시 순회
    names = ["Alice", "Bob", "Charlie"]
    scores = [95, 82, 78]
    log.info("[2-2] zip:")
    for name, sc in zip(names, scores):
        log.info("  %s → %d점", name, sc)

    # ──────────────────────────────────────────────────────────────────────────
    # 2-3. while 반복문
    # ──────────────────────────────────────────────────────────────────────────
    count = 0
    while count < 3:
        log.info("[2-3] while count=%d", count)
        count += 1  # count++는 파이썬에 없음 → count += 1 사용

    # ──────────────────────────────────────────────────────────────────────────
    # 2-4. break / continue / pass
    #   - break   : 반복문 즉시 탈출
    #   - continue: 현재 iteration 건너뛰고 다음으로
    #   - pass    : 아무것도 하지 않음 (자리 채우기용, Java에 없는 키워드)
    # ──────────────────────────────────────────────────────────────────────────
    log.info("[2-4] break 예시:")
    for i in range(10):
        if i == 5:
            log.info("  i=5 에서 break")
            break
        log.info("  i=%d", i)

    log.info("[2-4] continue 예시 (홀수만 출력):")
    for i in range(6):
        if i % 2 == 0:
            continue  # 짝수는 건너뜀
        log.info("  홀수: %d", i)

    # pass: 나중에 구현할 예정인 코드 자리 표시
    def todo_function():
        pass  # TODO: 나중에 구현 - pass 없으면 IndentationError 발생

    # ──────────────────────────────────────────────────────────────────────────
    # 2-5. for-else / while-else (파이썬 고유 문법)
    #   - else 블록은 break 없이 반복문이 정상 종료될 때만 실행됨
    #   - 검색 결과를 flag 없이 처리할 때 유용
    # ──────────────────────────────────────────────────────────────────────────
    target = 99
    numbers = [1, 5, 7, 23, 41]

    log.info("[2-5] for-else: %d 찾기", target)
    for num in numbers:
        if num == target:
            log.info("  찾았다: %d (break → else 실행 안 됨)", num)
            break
    else:
        # break 없이 for 루프가 끝나면 실행
        log.info("  %d를 못 찾음 (else 실행됨)", target)

    log.info("=== [단계2] 완료 ===")

    return (
        "[단계2 완료] 제어문\n"
        "- if/elif/else: Java의 else if → elif\n"
        "- 삼항연산자: 값 if 조건 else 값2\n"
        "- for in: 모든 iterable 순회\n"
        "- range(start, stop, step): 숫자 범위 생성\n"
        "- enumerate(iterable, start=0): 인덱스 + 값\n"
        "- zip(a, b): 두 iterable 동시 순회\n"
        "- break/continue/pass: 흐름 제어\n"
        "- for-else: break 없이 끝나면 else 실행 (파이썬 고유)\n"
    )
