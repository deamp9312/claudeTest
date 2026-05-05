package com.example.server5.controller;

import com.example.server5.domain.entity.User;
import com.example.server5.dto.UserSummaryDto;
import com.example.server5.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 유저 생성 → DB 저장 + Redis 캐시 저장
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.createUser(body.get("email"), body.get("name")));
    }

    // 단건 조회 → Redis 캐시 우선 (없으면 DB)
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // 목록 조회 → DTO Projection + Redis 캐시 (엔티티 전체 로드 없음)
    @GetMapping("/summary")
    public ResponseEntity<List<UserSummaryDto>> getAllSummary() {
        return ResponseEntity.ok(userService.findAllSummary());
    }

    // 이름 수정 → 벌크 UPDATE + 캐시 무효화
    @PatchMapping("/{id}/name")
    public ResponseEntity<Void> updateName(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.updateUserName(id, body.get("name"));
        return ResponseEntity.ok().build();
    }

    // Redis Hash 구조로 이름만 조회 (전체 역직렬화 없음)
    @GetMapping("/{id}/name")
    public ResponseEntity<String> getNameFromCache(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserNameFromHash(id));
    }
}
