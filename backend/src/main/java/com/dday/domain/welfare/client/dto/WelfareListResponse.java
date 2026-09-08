package com.dday.domain.welfare.client.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록조회 응답 루트 {@code <wantedList>}.
 *
 * <p>표준 data.go.kr 봉투(&lt;response&gt;&lt;header&gt;…)가 아니라 이 API 고유 구조다.
 * 성공은 {@code <resultCode>0</resultCode>} — 표준의 {@code 00}이 아니다.
 * 우리가 안 쓰는 엘리먼트는 JAXB가 조용히 무시한다.
 */
@Getter
@Setter
@XmlRootElement(name = "wantedList")
@XmlAccessorType(XmlAccessType.FIELD)
public class WelfareListResponse {

    private int totalCount;
    private int pageNo;
    private int numOfRows;
    private String resultCode;
    private String resultMessage;

    /** {@code <servList>}가 래퍼 없이 반복된다. */
    @XmlElement(name = "servList")
    private List<WelfareListItem> servList = new ArrayList<>();

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    public List<WelfareListItem> servListOrEmpty() {
        return servList == null ? List.of() : servList;
    }
}
