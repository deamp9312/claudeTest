"""
[단계 5] 객체지향 프로그래밍(OOP) - 클래스, 상속, 다형성, 매직 메서드

핵심 개념:
  - 파이썬의 모든 것은 객체 (int, str, 함수까지도)
  - self: Java의 this와 동일 (명시적으로 첫 번째 인자에 써야 함)
  - __init__: 생성자 (Java의 Constructor)
  - 매직 메서드(Dunder method): __로 감싼 특수 메서드 - 연산자 오버로딩 등에 사용
"""

import logging
from typing import Optional

log = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────────────
# 5-1. 기본 클래스 정의
# ──────────────────────────────────────────────────────────────────────────────
class Animal:
    # 클래스 변수: 모든 인스턴스가 공유 (Java의 static 변수)
    species_count = 0

    def __init__(self, name: str, age: int):
        # 인스턴스 변수: 각 객체마다 독립적으로 가짐
        # self.변수명 으로 선언 (Java의 this.field = value)
        self.name = name
        self.age = age
        Animal.species_count += 1   # 클래스 변수는 클래스명.변수로 접근

    # 인스턴스 메서드: 첫 번째 인자로 항상 self 필요
    def speak(self) -> str:
        return f"{self.name}이(가) 소리를 냅니다"

    def get_info(self) -> str:
        return f"동물: {self.name}, 나이: {self.age}살"

    # __str__: print(객체) 또는 str(객체) 호출 시 사용 (Java의 toString())
    def __str__(self) -> str:
        return f"Animal(name={self.name}, age={self.age})"

    # __repr__: 개발자용 표현 (디버깅, repr() 호출 시)
    def __repr__(self) -> str:
        return f"Animal('{self.name}', {self.age})"

    # 클래스 메서드: cls로 클래스 자체에 접근 (Java의 static method와 유사)
    @classmethod
    def get_count(cls) -> int:
        return cls.species_count

    # 정적 메서드: self/cls 없음, 단순 유틸리티 함수
    @staticmethod
    def is_valid_age(age: int) -> bool:
        return 0 <= age <= 100


# ──────────────────────────────────────────────────────────────────────────────
# 5-2. 상속(Inheritance)
#   - class 자식클래스(부모클래스):
#   - super(): 부모 클래스 참조 (Java의 super와 동일)
# ──────────────────────────────────────────────────────────────────────────────
class Dog(Animal):  # Animal을 상속

    def __init__(self, name: str, age: int, breed: str):
        super().__init__(name, age)  # 부모 __init__ 호출
        self.breed = breed           # 자식만의 추가 속성

    # 메서드 오버라이딩 (Override)
    def speak(self) -> str:
        return f"{self.name}이(가) 왈왈! 짖습니다"

    def fetch(self) -> str:
        return f"{self.name}이(가) 공을 가져옵니다"

    def __str__(self) -> str:
        return f"Dog(name={self.name}, breed={self.breed})"


class Cat(Animal):

    def __init__(self, name: str, age: int, indoor: bool = True):
        super().__init__(name, age)
        self.indoor = indoor

    def speak(self) -> str:
        return f"{self.name}이(가) 야옹~ 울습니다"

    def purr(self) -> str:
        return f"{self.name}이(가) 그르릉..."


# ──────────────────────────────────────────────────────────────────────────────
# 5-3. 다중 상속 (Multiple Inheritance)
#   - 파이썬은 다중 상속 지원 (Java는 인터페이스로만 가능)
#   - MRO(Method Resolution Order): C3 선형화 알고리즘으로 메서드 탐색 순서 결정
# ──────────────────────────────────────────────────────────────────────────────
class Flyable:
    def fly(self) -> str:
        return "날아갑니다"


class Swimmable:
    def swim(self) -> str:
        return "헤엄칩니다"


class Duck(Animal, Flyable, Swimmable):  # 다중 상속

    def speak(self) -> str:
        return f"{self.name}이(가) 꽥꽥!"


# ──────────────────────────────────────────────────────────────────────────────
# 5-4. 프로퍼티(Property) - getter/setter
#   - @property: getter 정의 (Java의 getXxx())
#   - @속성명.setter: setter 정의 (Java의 setXxx())
#   - 외부에서는 메서드처럼 보이지 않고 속성처럼 접근 가능
# ──────────────────────────────────────────────────────────────────────────────
class Circle:

    def __init__(self, radius: float):
        self._radius = radius  # 관례상 _ 접두사 = private (강제는 아님)

    @property
    def radius(self) -> float:
        """getter: circle.radius 로 접근"""
        return self._radius

    @radius.setter
    def radius(self, value: float) -> None:
        """setter: circle.radius = 5 로 설정"""
        if value < 0:
            raise ValueError("반지름은 0 이상이어야 합니다")
        self._radius = value

    @property
    def area(self) -> float:
        """계산된 프로퍼티 (setter 없음 = 읽기 전용)"""
        import math
        return math.pi * self._radius ** 2


# ──────────────────────────────────────────────────────────────────────────────
# 5-5. 매직 메서드(Magic Method / Dunder Method)
#   - __로 시작하고 끝나는 특수 메서드
#   - 파이썬 내장 연산자(+, >, len 등)의 동작을 클래스에서 정의
# ──────────────────────────────────────────────────────────────────────────────
class Vector:
    """2D 벡터 - 매직 메서드 데모"""

    def __init__(self, x: float, y: float):
        self.x = x
        self.y = y

    def __str__(self) -> str:           # str(v), print(v)
        return f"Vector({self.x}, {self.y})"

    def __repr__(self) -> str:          # repr(v), 디버깅
        return f"Vector({self.x!r}, {self.y!r})"

    def __add__(self, other: "Vector") -> "Vector":   # v1 + v2
        return Vector(self.x + other.x, self.y + other.y)

    def __sub__(self, other: "Vector") -> "Vector":   # v1 - v2
        return Vector(self.x - other.x, self.y - other.y)

    def __mul__(self, scalar: float) -> "Vector":     # v * 3
        return Vector(self.x * scalar, self.y * scalar)

    def __eq__(self, other: object) -> bool:          # v1 == v2
        if not isinstance(other, Vector):
            return NotImplemented
        return self.x == other.x and self.y == other.y

    def __len__(self) -> int:           # len(v) → 항상 2 (2D 벡터)
        return 2

    def __abs__(self) -> float:         # abs(v) → 크기
        return (self.x ** 2 + self.y ** 2) ** 0.5

    def __getitem__(self, index: int) -> float:   # v[0], v[1]
        if index == 0:
            return self.x
        elif index == 1:
            return self.y
        raise IndexError("Vector index out of range")


# ──────────────────────────────────────────────────────────────────────────────
# 5-6. 추상 클래스(Abstract Class)
#   - abc 모듈의 ABC, abstractmethod 사용
#   - Java의 abstract class / interface와 유사
# ──────────────────────────────────────────────────────────────────────────────
from abc import ABC, abstractmethod


class Shape(ABC):  # ABC = Abstract Base Class

    @abstractmethod
    def area(self) -> float:
        """면적 계산 - 반드시 구현해야 함"""
        ...  # 또는 pass

    @abstractmethod
    def perimeter(self) -> float:
        """둘레 계산 - 반드시 구현해야 함"""
        ...

    def describe(self) -> str:
        # 추상 클래스도 구현된 메서드를 가질 수 있음
        return f"넓이: {self.area():.2f}, 둘레: {self.perimeter():.2f}"


class Rectangle(Shape):

    def __init__(self, width: float, height: float):
        self.width = width
        self.height = height

    def area(self) -> float:
        return self.width * self.height

    def perimeter(self) -> float:
        return 2 * (self.width + self.height)


def run() -> str:
    log.info("=== [단계5] OOP 시작 ===")

    # 5-1 기본 클래스
    animal = Animal("동물", 5)
    log.info("[5-1] %s", animal)
    log.info("[5-1] %s", animal.get_info())
    log.info("[5-1] 클래스 변수: species_count=%d", Animal.get_count())
    log.info("[5-1] 정적 메서드: is_valid_age(5)=%s", Animal.is_valid_age(5))

    # 5-2 상속 / 다형성
    dog = Dog("멍멍이", 3, "진돗개")
    cat = Cat("야옹이", 2)
    log.info("[5-2] dog: %s", dog)
    log.info("[5-2] dog.speak(): %s", dog.speak())
    log.info("[5-2] cat.speak(): %s", cat.speak())

    # 다형성: 부모 타입 변수에 자식 객체 담기
    animals: list[Animal] = [dog, cat, Animal("새", 1)]
    for a in animals:
        log.info("[5-2] 다형성: %s", a.speak())  # 각 클래스의 speak() 호출

    # isinstance 체크
    log.info("[5-2] isinstance(dog, Animal)=%s, isinstance(dog, Cat)=%s",
             isinstance(dog, Animal), isinstance(dog, Cat))

    # 5-3 다중 상속
    duck = Duck("오리", 2)
    log.info("[5-3] duck: %s %s %s", duck.speak(), duck.fly(), duck.swim())
    log.info("[5-3] MRO: %s", [cls.__name__ for cls in Duck.__mro__])

    # 5-4 프로퍼티
    c = Circle(5.0)
    log.info("[5-4] 반지름=%.1f, 넓이=%.2f", c.radius, c.area)
    c.radius = 10.0   # setter 호출
    log.info("[5-4] 반지름 변경 후: %.1f, 넓이=%.2f", c.radius, c.area)

    # 5-5 매직 메서드
    v1 = Vector(1, 2)
    v2 = Vector(3, 4)
    log.info("[5-5] v1=%s, v2=%s", v1, v2)
    log.info("[5-5] v1+v2=%s, v1-v2=%s, v1*2=%s", v1 + v2, v1 - v2, v1 * 2)
    log.info("[5-5] abs(v2)=%.2f, len(v1)=%d, v2[0]=%.1f", abs(v2), len(v1), v2[0])
    log.info("[5-5] v1==v1: %s, v1==v2: %s", v1 == v1, v1 == v2)

    # 5-6 추상 클래스
    rect = Rectangle(4, 5)
    log.info("[5-6] 사각형: %s", rect.describe())
    # shape = Shape()  # TypeError: 추상 클래스 직접 인스턴스화 불가

    log.info("=== [단계5] 완료 ===")

    return (
        "[단계5 완료] 객체지향 프로그래밍\n"
        "- class Animal: / def __init__(self, ...): → 생성자\n"
        "- 클래스 변수 vs 인스턴스 변수 / @classmethod / @staticmethod\n"
        "- class Dog(Animal): + super().__init__() → 상속\n"
        "- 다중 상속: class Duck(Animal, Flyable, Swimmable)\n"
        "- @property / @속성.setter → getter/setter (속성처럼 접근)\n"
        "- 매직 메서드: __str__ __add__ __eq__ __len__ 등 연산자 오버로딩\n"
        "- 추상 클래스: class Shape(ABC) + @abstractmethod\n"
    )
