package com.dday;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DdayApplication {

    /**
     * JVM 기본 타임존을 KST로 고정한다.
     *
     * <p>이걸 안 하면 로컬(맥, KST)과 서버 컨테이너(UTC)에서 {@code LocalDateTime.now()}가
     * 9시간 어긋난다. "로컬에선 맞는데 배포하면 날짜가 하루 밀린다"의 대부분이 이 원인이다.
     * DB 쪽은 docker-compose의 {@code --default-time-zone=+09:00}이 맞춘다.
     */
    @PostConstruct
    public void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(DdayApplication.class, args);
    }
}
