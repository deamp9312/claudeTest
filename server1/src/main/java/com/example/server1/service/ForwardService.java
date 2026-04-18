package com.example.server1.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ForwardService {

    private final RestClient restClient;

    public ForwardService(@Value("${server2.url}") String server2Url) {
        this.restClient = RestClient.builder()
                .baseUrl(server2Url)
                .build();
    }

    public void forwardToServer2(String message) {
        log.info("[Server1] Server2로 메시지 전달 중: {}", message);
        String response = restClient.post()
                .uri("/api/receive")
                .body(message)
                .retrieve()
                .body(String.class);
        log.info("[Server1] Server2 응답: {}", response);
    }
}
