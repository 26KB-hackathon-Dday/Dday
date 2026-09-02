package com.dday.domain.welfare.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 복지 API 전용 {@link RestClient}. 타임아웃은 {@code welfare.api.*}에서 온다.
 * XML 파싱은 {@link WelfareXml}(정적)이 담당하므로 여기서 만들 빈은 클라이언트 하나뿐이다.
 */
@Configuration
public class WelfareClientConfig {

    @Bean
    public RestClient welfareRestClient(
            @Value("${welfare.api.base-url}") String baseUrl,
            @Value("${welfare.api.connect-timeout}") Duration connectTimeout,
            @Value("${welfare.api.read-timeout}") Duration readTimeout) {

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
