package com.example.server2.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class MessageController {

    @PostMapping("/receive")
    public String receive(@RequestBody String message) {
        log.info("[Server2] Server1로부터 메시지 수신: {}", message);
        return "Server2 로그 완료: " + message;
    }
}
