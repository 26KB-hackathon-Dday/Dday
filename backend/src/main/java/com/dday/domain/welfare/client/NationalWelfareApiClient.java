package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.WelfareDetailResponse;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.client.dto.WelfareListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 한국사회보장정보원_중앙부처복지서비스 API 클라이언트.
 *
 * <p>수집 쿼리는 {@code srchKeyCode=003 & searchWrd=청년 & lifeArray=004} 고정이다
 * (docs/welfare-api/NOTES.md §4). 이 API는 HTTP 200에 {@code <resultCode>} 로 실패를 알리므로
 * 상태코드만 보고 넘어가면 안 된다.
 */
@Slf4j
@Component
public class NationalWelfareApiClient {

    private static final String LIST_PATH = "/NationalWelfarelistV001";
    private static final String DETAIL_PATH = "/NationalWelfaredetailedV001";
    private static final String SEARCH_KEY_CODE = "003";
    private static final String SEARCH_WORD = "청년";

    private final RestClient restClient;
    private final String serviceKey;
    private final String lifeCode;
    private final int pageSize;

    public NationalWelfareApiClient(RestClient welfareRestClient,
                                    @Value("${welfare.api.service-key}") String serviceKey,
                                    @Value("${welfare.api.life-code}") String lifeCode,
                                    @Value("${welfare.api.page-size}") int pageSize) {
        this.restClient = welfareRestClient;
        this.serviceKey = serviceKey;
        this.lifeCode = lifeCode;
        this.pageSize = pageSize;
    }

    /**
     * 청년 대상 목록 한 페이지. {@link WelfareListResponse#getTotalCount()}로 다음 페이지 유무를
     * 판단한다.
     *
     * @throws WelfareApiException 호출 실패, 파싱 실패, {@code resultCode != 0}
     */
    public WelfareListResponse fetchYouthList(int pageNo) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new WelfareApiException("welfare.api.service-key(WELFARE_API_KEY)가 비어 있다. 수집을 실행할 수 없다.");
        }

        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(LIST_PATH)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("srchKeyCode", SEARCH_KEY_CODE)
                            .queryParam("searchWrd", SEARCH_WORD)
                            .queryParam("lifeArray", lifeCode)
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", pageSize)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new WelfareApiException("복지 목록 API 호출 실패 (pageNo=" + pageNo + ")", e);
        }

        if (body == null || body.isBlank()) {
            throw new WelfareApiException("복지 목록 API 응답이 비어 있다 (pageNo=" + pageNo + ")");
        }

        WelfareListResponse response = WelfareXml.readListResponse(body);
        if (!response.isSuccess()) {
            throw new WelfareApiException(
                    "복지 목록 API 오류 응답 (pageNo=" + pageNo + ", resultCode=" + response.getResultCode()
                            + ", resultMessage=" + response.getResultMessage() + ")");
        }

        // 원본 보존 — 항목을 다시 XML로 되돌려 저장한다. (페이지 XML을 항목별로 자르지 않는다)
        for (WelfareListItem item : response.servListOrEmpty()) {
            item.setRawXml(WelfareXml.writeItem(item));
        }
        log.debug("복지 목록 pageNo={} totalCount={} 수신 {}건", pageNo, response.getTotalCount(),
                response.servListOrEmpty().size());
        return response;
    }

    /**
     * 중앙부처 제도 상세 1건. 상세보강 Step이 금액·기준연도를 뽑으려고 호출한다.
     *
     * @throws WelfareApiException 호출/파싱 실패, {@code resultCode != 0}
     */
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
            throw new WelfareApiException("복지 상세 API 호출 실패 (servId=" + servId + ")", e);
        }
        if (body == null || body.isBlank()) {
            throw new WelfareApiException("복지 상세 API 응답이 비어 있다 (servId=" + servId + ")");
        }
        WelfareDetailResponse response = WelfareXml.readDetailResponse(body);
        if (!response.isSuccess()) {
            throw new WelfareApiException("복지 상세 API 오류 응답 (servId=" + servId
                    + ", resultCode=" + response.getResultCode() + ")");
        }
        response.setRawXml(body);
        return response;
    }
}
