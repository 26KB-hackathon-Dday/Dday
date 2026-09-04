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
 * 지자체복지서비스 목록조회 응답 루트 {@code <wantedList>}.
 *
 * <p>루트 엘리먼트 이름은 CENTRAL({@link WelfareListResponse})과 같은 {@code <wantedList>}지만,
 * 항목({@code <servList>})의 필드 구성이 달라 별도 DTO로 둔다 — docs/welfare-api/NOTES.md §11-3.
 * 성공 판정은 CENTRAL과 동일하게 {@code <resultCode>0</resultCode>}.
 */
@Getter
@Setter
@XmlRootElement(name = "wantedList")
@XmlAccessorType(XmlAccessType.FIELD)
public class LcgvWelfareListResponse {

    private int totalCount;
    private int pageNo;
    private int numOfRows;
    private String resultCode;
    private String resultMessage;

    @XmlElement(name = "servList")
    private List<LcgvWelfareListItem> servList = new ArrayList<>();

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    public List<LcgvWelfareListItem> servListOrEmpty() {
        return servList == null ? List.of() : servList;
    }
}
