"""
파이썬 학습 서버 (server4)
  server1의 ThreadLearningController 역할을 Flask로 구현

API:
  GET /python/1   → 기본 문법 (변수, 자료형, 연산자)
  GET /python/2   → 제어문 (if, for, while)
  GET /python/3   → 함수 (def, lambda, 클로저)
  GET /python/4   → 컬렉션 (list, dict, tuple, set, comprehension)
  GET /python/5   → OOP (클래스, 상속, 매직 메서드)
  GET /python/6   → 예외처리 (try/except, 컨텍스트 매니저)
  GET /python/7   → 고급 기능 (데코레이터, 제너레이터, dataclass)
  GET /python/all → 전체 단계 순차 실행

실행 방법:
  pip install flask
  python main.py
  curl http://localhost:5000/python/1
"""

import logging
import sys
import os

# 로깅 설정 (server1의 Spring Boot 로그 형식과 유사하게)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    stream=sys.stdout,
)
log = logging.getLogger(__name__)

from flask import Flask, jsonify
from python import (
    step1_basics,
    step2_control_flow,
    step3_functions,
    step4_collections,
    step5_oop,
    step6_exceptions,
    step7_advanced,
)

app = Flask(__name__)


# ──────────────────────────────────────────────────────────────────────────────
# 공통 응답 핸들러
# ──────────────────────────────────────────────────────────────────────────────
def run_step(step_func, step_name: str):
    """각 단계 실행 후 결과 반환 (예외는 JSON으로 응답)"""
    log.info("=== API 호출: %s ===", step_name)
    try:
        result = step_func()
        return result, 200, {"Content-Type": "text/plain; charset=utf-8"}
    except Exception as e:
        log.error("단계 실행 오류: %s", e, exc_info=True)
        return jsonify({"error": str(e)}), 500


# ──────────────────────────────────────────────────────────────────────────────
# 라우팅 (server1의 @GetMapping 역할)
#   /python/N → step N 실행
# ──────────────────────────────────────────────────────────────────────────────

@app.get("/python/1")
def step1():
    return run_step(step1_basics.run, "단계1 - 기본 문법")


@app.get("/python/2")
def step2():
    return run_step(step2_control_flow.run, "단계2 - 제어문")


@app.get("/python/3")
def step3():
    return run_step(step3_functions.run, "단계3 - 함수")


@app.get("/python/4")
def step4():
    return run_step(step4_collections.run, "단계4 - 컬렉션")


@app.get("/python/5")
def step5():
    return run_step(step5_oop.run, "단계5 - OOP")


@app.get("/python/6")
def step6():
    return run_step(step6_exceptions.run, "단계6 - 예외처리")


@app.get("/python/7")
def step7():
    return run_step(step7_advanced.run, "단계7 - 고급 기능")


@app.get("/python/all")
def all_steps():
    """전체 단계 순차 실행 (server1의 /thread/all 과 동일한 역할)"""
    log.info("=== API 호출: 전체 단계 실행 ===")
    steps = [
        (step1_basics.run, "단계1"),
        (step2_control_flow.run, "단계2"),
        (step3_functions.run, "단계3"),
        (step4_collections.run, "단계4"),
        (step5_oop.run, "단계5"),
        (step6_exceptions.run, "단계6"),
        (step7_advanced.run, "단계7"),
    ]

    results = []
    for func, name in steps:
        try:
            results.append(func())
        except Exception as e:
            results.append(f"[{name} 오류] {e}")

    combined = "\n".join(results)
    return combined, 200, {"Content-Type": "text/plain; charset=utf-8"}


@app.get("/")
def index():
    return (
        "파이썬 학습 서버 (server4)\n\n"
        "  GET /python/1  → 기본 문법\n"
        "  GET /python/2  → 제어문\n"
        "  GET /python/3  → 함수\n"
        "  GET /python/4  → 컬렉션\n"
        "  GET /python/5  → OOP\n"
        "  GET /python/6  → 예외처리\n"
        "  GET /python/7  → 고급 기능\n"
        "  GET /python/all → 전체 실행\n"
    ), 200, {"Content-Type": "text/plain; charset=utf-8"}


if __name__ == "__main__":
    log.info("파이썬 학습 서버 시작 (port=5000)")
    # debug=False: 운영에서는 반드시 False (자동 재시작, 디버거 비활성화)
    app.run(host="0.0.0.0", port=5000, debug=True)
