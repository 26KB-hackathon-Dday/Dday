package com.dday.domain.welfare.batch;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * welfare 도메인의 스케줄링을 켠다.
 *
 * <p>전역이 아니라 도메인 폴더 안에 두는 이유: 다른 팀원이 global을 건드릴 일을 줄이려는 것
 * (AGENTS.md — "도메인 하나 = 폴더 하나"). 다른 도메인이 스케줄링을 쓰게 되면 그때 global로 올린다.
 */
@Configuration
@EnableScheduling
public class WelfareBatchConfig {
}
