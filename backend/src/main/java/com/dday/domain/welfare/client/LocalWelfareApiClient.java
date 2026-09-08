package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.LcgvWelfareListItem;
import com.dday.domain.welfare.client.dto.LcgvWelfareListResponse;
import com.dday.domain.welfare.client.dto.WelfareDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 한국사회보장정보원_지자체복지서비스 API 클라이언트 — docs/welfare-api/NOTES.md §11.
 *
 * <p>수집 쿼리는 {@code lifeArray=004 & searchWrd=자립준비청년} 고정이다({@code srchKeyCode}는
 * 안 보낸다 — CENTRAL과 달리 그래도 {@code searchWrd}가 먹는다). 좁게 긁으므로 결과는 한 자릿수다.
 *
 * <p>{@link NationalWelfareApiClient}와 구조가 같다 — HTTP 200에 {@code <resultCode>}로 실패를
 * 알리므로 상태코드만 보면 안 된다.
 */
@Slf4j
@Component
public class LocalWelfareApiClient {

    private static final String LIST_PATH = "/LcgvWelfarelist";
    private static final String DETAIL_PATH = "/LcgvWelfaredetailed";

    private final RestClient restClient;
    private final String serviceKey;
    private final String lifeCode;
    private final String searchWord;
    private final int pageSize;

    public LocalWelfareApiClient(RestClient lcgvWelfareRestClient,
                                 @Value("${welfare.api.service-key}") String serviceKey,
                                 @Value("${welfare.api.life-code}") String lifeCode,
                                 @Value("${welfare.api.local-search-word}") String searchWord,
                                 @Value("${welfare.api.page-size}") int pageSize) {
        this.restClient = lcgvWelfareRestClient;
        this.serviceKey = serviceKey;
        this.lifeCode = lifeCode;
        this.searchWord = searchWord;
        this.pageSize = pageSize;
    }

    /**
     * 자립준비청년 대상 지자체 제도 목록 한 페이지.
     *
     * @throws WelfareApiException 호출 실패, 파싱 실패, {@code resultCode != 0}
     */
    public LcgvWelfareListResponse fetchList(int pageNo) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new WelfareApiException("welfare.api.service-key(WELFARE_API_KEY)가 비어 있다. 수집을 실행할 수 없다.");
        }

        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(LIST_PATH)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("searchWrd", searchWord)
                            .queryParam("lifeArray", lifeCode)
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", pageSize)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new WelfareApiException("지자체 복지 목록 API 호출 실패 (pageNo=" + pageNo + ")", e);
        }

        if (body == null || body.isBlank()) {
            throw new WelfareApiException("지자체 복지 목록 API 응답이 비어 있다 (pageNo=" + pageNo + ")");
        }

        LcgvWelfareListResponse response = WelfareXml.readLcgvListResponse(body);
        if (!response.isSuccess()) {
            throw new WelfareApiException(
                    "지자체 복지 목록 API 오류 응답 (pageNo=" + pageNo + ", resultCode=" + response.getResultCode()
                            + ", resultMessage=" + response.getResultMessage() + ")");
        }

        for (LcgvWelfareListItem item : response.servListOrEmpty()) {
            item.setRawXml(WelfareXml.writeLcgvItem(item));
        }
        log.debug("지자체 복지 목록 pageNo={} totalCount={} 수신 {}건", pageNo, response.getTotalCount(),
                response.servListOrEmpty().size());
        return response;
    }

    /** 지자체 제도 상세 1건. 루트는 중앙과 같은 {@code <wantedDtl>}. */
    public WelfareDetailResponse fetchDetail(String servId) {
        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DETAIL_PATH)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("servId", servId)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new WelfareApiException("지자체 복지 상세 API 호출 실패 (servId=" + servId + ")", e);
        }
        if (body == null || body.isBlank()) {
            throw new WelfareApiException("지자체 복지 상세 API 응답이 비어 있다 (servId=" + servId + ")");
        }
        WelfareDetailResponse response = WelfareXml.readDetailResponse(body);
        if (!response.isSuccess()) {
            throw new WelfareApiException("지자체 복지 상세 API 오류 응답 (servId=" + servId
                    + ", resultCode=" + response.getResultCode() + ")");
        }
        response.setRawXml(body);
        return response;
    }
}
