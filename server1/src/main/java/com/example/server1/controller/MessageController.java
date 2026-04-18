package com.example.server1.controller;

import com.example.server1.service.ForwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final ForwardService forwardService;

    @PostMapping("/send")
    public String send(@RequestParam String message) {
        log.info("[Server1] 메시지 수신: {}", message);
        forwardService.forwardToServer2(message);
        return "Server1 수신 완료, Server2로 전달: " + message;
    }
}
