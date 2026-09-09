package com.dday.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 회원별 SSE 구독 연결을 들고 있다가 알림을 실어 보낸다.
 *
 * <p>연결은 서버 메모리에만 있다 — 인스턴스가 여러 대로 늘어나면(수평 확장) 이 방식은
 * 안 통한다. 그때는 Redis Pub/Sub 등으로 인스턴스 간에 이벤트를 전달해야 한다.
 * 지금은 단일 인스턴스라 문제없다.
 *
 * <p>한 회원이 탭을 여러 개 열면 구독도 여러 개 생긴다. 전부에게 보낸다 — 어느 탭이
 * "진짜"인지 서버는 알 방법이 없다.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    /**
     * 프록시(nginx 등)가 응답이 없는 연결을 끊는 경우가 있어, 타임아웃을 넉넉히 잡고
     * 그 안에서는 {@link #sendHeartbeat()}로 계속 살아있음을 알린다. 타임아웃이 지나면
     * 클라이언트의 {@code EventSource}가 알아서 재연결한다.
     */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = emittersByUserId.computeIfAbsent(
                userId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        // 연결 직후 아무것도 안 보내면 일부 프록시가 응답을 버퍼링해서 클라이언트가
        // "연결됨"을 못 알아챈다. 빈 이벤트 하나로 스트림을 즉시 흘려보낸다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            remove(userId, emitter);
        }

        return emitter;
    }

    public void send(Long userId, String eventName, Object data) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    /** 15초마다 핑을 보내 유휴 연결로 오인되어 끊기지 않게 한다. */
    @Scheduled(fixedRate = 15_000)
    public void sendHeartbeat() {
        emittersByUserId.forEach((userId, emitters) -> {
            for (SseEmitter emitter : List.copyOf(emitters)) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
