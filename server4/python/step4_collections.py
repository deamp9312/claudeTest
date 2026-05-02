"""
[단계 4] 컬렉션 - list, dict, tuple, set, comprehension

핵심 개념:
  - list  : 순서 O, 중복 O, 변경 O  (Java의 ArrayList)
  - dict  : 순서 O(3.7+), 중복 키 X, 변경 O  (Java의 HashMap)
  - tuple : 순서 O, 중복 O, 변경 X  (불변 list - 해시 가능)
  - set   : 순서 X, 중복 X, 변경 O  (Java의 HashSet)
  - 컴프리헨션(Comprehension): 컬렉션을 한 줄로 생성하는 파이썬 고유 문법
"""

import logging

log = logging.getLogger(__name__)


def demo_list():
    """리스트 - 가장 많이 쓰는 컬렉션"""
    log.info("── [4-1] list ──")

    # 생성
    fruits = ["사과", "바나나", "딸기"]
    mixed = [1, "hello", True, None, 3.14]  # 여러 타입 혼합 가능 (Java는 제네릭 필요)
    empty = []

    # 인덱싱 / 슬라이싱
    log.info("fruits[0]=%s, fruits[-1]=%s", fruits[0], fruits[-1])
    log.info("fruits[1:]=%s, fruits[::-1]=%s", fruits[1:], fruits[::-1])

    # 추가 / 삽입 / 삭제
    fruits.append("포도")           # 끝에 추가 (Java의 add())
    fruits.insert(1, "키위")        # index=1 위치에 삽입
    fruits.remove("바나나")         # 값으로 삭제 (없으면 ValueError)
    popped = fruits.pop()           # 마지막 요소 제거 후 반환
    popped_idx = fruits.pop(0)      # 특정 인덱스 요소 제거 후 반환
    log.info("popped=%s, popped_idx=%s, fruits=%s", popped, popped_idx, fruits)

    # 검색
    nums = [3, 1, 4, 1, 5, 9, 2, 6]
    log.info("index(1)=%d, count(1)=%d", nums.index(1), nums.count(1))

    # 정렬
    nums.sort()                         # 제자리 정렬 (원본 변경)
    log.info("sort(): %s", nums)
    nums.sort(reverse=True)             # 내림차순
    log.info("sort(reverse): %s", nums)
    sorted_nums = sorted(nums)          # 새 리스트 반환 (원본 유지)
    log.info("sorted(): %s", sorted_nums)

    # 리스트 합치기
    a = [1, 2, 3]
    b = [4, 5, 6]
    combined = a + b        # 새 리스트 생성
    a.extend(b)             # a에 b를 이어붙임 (in-place)
    log.info("+ : %s, extend: %s", combined, a)

    # 길이 / 존재 여부
    log.info("len=%d, '사과' in fruits → %s", len(fruits), "사과" in fruits)


def demo_dict():
    """딕셔너리 - 키-값 쌍의 컬렉션"""
    log.info("── [4-2] dict ──")

    # 생성
    person = {"name": "홍길동", "age": 25, "city": "서울"}
    empty = {}
    from_keys = dict.fromkeys(["a", "b", "c"], 0)  # 같은 기본값으로 초기화
    log.info("from_keys: %s", from_keys)

    # 접근
    log.info("person['name']=%s", person["name"])

    # get(): 키 없을 때 기본값 반환 (KeyError 방지)
    # person["없는키"] 는 KeyError 발생 → get() 권장
    log.info("get('phone', 'N/A')=%s", person.get("phone", "N/A"))

    # 추가 / 수정 / 삭제
    person["email"] = "hong@test.com"   # 키 없으면 추가, 있으면 수정
    person["age"] = 26                  # 수정
    del person["city"]                  # 키로 삭제
    removed = person.pop("email", None) # 삭제 후 반환 (없으면 None)
    log.info("person=%s, removed=%s", person, removed)

    # 순회
    log.info("keys: %s", list(person.keys()))
    log.info("values: %s", list(person.values()))
    log.info("items (키-값 쌍):")
    for key, value in person.items():   # 가장 많이 쓰는 순회 방법
        log.info("  %s: %s", key, value)

    # 존재 확인
    log.info("'name' in person → %s", "name" in person)  # 키 존재 여부

    # 딕셔너리 병합 (Python 3.9+)
    defaults = {"port": 8080, "host": "localhost"}
    overrides = {"port": 443}
    merged = defaults | overrides   # | 연산자로 병합 (오른쪽이 우선)
    log.info("merged: %s", merged)

    # setdefault(): 키 없을 때만 기본값 설정
    person.setdefault("role", "user")   # 없으면 추가
    person.setdefault("name", "변경X")  # 이미 있으면 무시
    log.info("setdefault: %s", person)


def demo_tuple():
    """튜플 - 불변(immutable) 리스트"""
    log.info("── [4-3] tuple ──")

    # 생성: 괄호 또는 쉼표만으로 생성
    point = (3, 7)
    rgb = (255, 128, 0)
    single = (42,)  # 요소 1개 튜플 → 반드시 쉼표 필요! (42)는 그냥 정수
    no_parens = 1, 2, 3  # 괄호 없이도 튜플

    log.info("point=%s, rgb=%s, single=%s", point, rgb, single)

    # 언패킹(Unpacking) - 튜플의 가장 강력한 특징
    x, y = point            # 변수에 각각 할당
    r, g, b = rgb
    first, *rest = [1, 2, 3, 4, 5]  # * 로 나머지를 리스트로
    log.info("x=%d, y=%d | r=%d, g=%d, b=%d", x, y, r, g, b)
    log.info("first=%d, rest=%s", first, rest)

    # 불변이라 딕셔너리 키로 사용 가능 (리스트는 불가)
    locations = {(37.5, 127.0): "서울", (35.1, 129.0): "부산"}
    log.info("locations[서울좌표]=%s", locations[(37.5, 127.0)])

    # 함수 반환값으로 자주 사용
    def get_range(numbers):
        return min(numbers), max(numbers)  # 튜플 반환

    lo, hi = get_range([3, 1, 4, 1, 5, 9])
    log.info("range: min=%d, max=%d", lo, hi)


def demo_set():
    """셋 - 중복 없는 집합"""
    log.info("── [4-4] set ──")

    # 생성
    a = {1, 2, 3, 4, 5}
    b = {3, 4, 5, 6, 7}
    from_list = set([1, 2, 2, 3, 3, 3])  # 중복 제거
    log.info("from_list: %s", from_list)  # {1, 2, 3}

    # 추가 / 삭제
    a.add(6)
    a.discard(10)  # 없어도 에러 없음 (remove()는 없으면 KeyError)

    # 집합 연산
    log.info("합집합 (|): %s", a | b)
    log.info("교집합 (&): %s", a & b)
    log.info("차집합 (-): %s", a - b)
    log.info("대칭차집합 (^): %s", a ^ b)  # 한쪽에만 있는 요소

    # 부분집합 확인
    log.info("{3,4} ⊆ a: %s", {3, 4}.issubset(a))

    # 중복 제거 관용구
    duplicates = [1, 2, 2, 3, 3, 3, 4]
    unique = list(set(duplicates))
    log.info("중복 제거: %s → %s", duplicates, sorted(unique))


def demo_comprehension():
    """컴프리헨션 - 한 줄로 컬렉션 생성"""
    log.info("── [4-5] comprehension ──")

    # 리스트 컴프리헨션: [표현식 for 변수 in 이터러블 if 조건]
    # Java: numbers.stream().map(...).filter(...).collect(Collectors.toList())
    squares = [x ** 2 for x in range(1, 6)]
    log.info("제곱: %s", squares)

    evens = [x for x in range(20) if x % 2 == 0]
    log.info("짝수: %s", evens)

    words = ["hello", "world", "python"]
    upper = [w.upper() for w in words]
    log.info("대문자: %s", upper)

    # 중첩 컴프리헨션 (2D → 1D 평탄화)
    matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    flat = [num for row in matrix for num in row]
    log.info("평탄화: %s", flat)

    # 딕셔너리 컴프리헨션: {키: 값 for 변수 in 이터러블}
    squared_dict = {x: x ** 2 for x in range(1, 6)}
    log.info("딕셔너리 컴프리헨션: %s", squared_dict)

    # 반전: {value: key for key, value in d.items()}
    word_to_num = {"one": 1, "two": 2, "three": 3}
    num_to_word = {v: k for k, v in word_to_num.items()}
    log.info("딕셔너리 반전: %s", num_to_word)

    # 셋 컴프리헨션: {표현식 for ...}
    unique_squares = {x ** 2 for x in [-2, -1, 0, 1, 2]}
    log.info("셋 컴프리헨션: %s", unique_squares)

    # 제너레이터 표현식: () 사용 → 지연 평가(lazy evaluation), 메모리 효율적
    # 한 번만 순회하면 되는 대용량 데이터에 적합
    gen = (x ** 2 for x in range(10))
    log.info("제너레이터 첫 3개: %s", [next(gen) for _ in range(3)])


def run() -> str:
    log.info("=== [단계4] 컬렉션 시작 ===")
    demo_list()
    demo_dict()
    demo_tuple()
    demo_set()
    demo_comprehension()
    log.info("=== [단계4] 완료 ===")

    return (
        "[단계4 완료] 컬렉션\n"
        "- list  : 순서O 중복O 변경O — append/insert/pop/sort\n"
        "- dict  : 키-값 쌍 — get(key, default)으로 안전하게 접근\n"
        "- tuple : 불변 리스트 — 언패킹, 딕셔너리 키로 사용 가능\n"
        "- set   : 중복X 순서X — 집합 연산(| & - ^) 지원\n"
        "- [x*2 for x in lst if 조건]: 리스트 컴프리헨션\n"
        "- {k: v for k,v in d.items()}: 딕셔너리 컴프리헨션\n"
        "- (x for x in lst): 제너레이터 표현식 (메모리 효율)\n"
    )
