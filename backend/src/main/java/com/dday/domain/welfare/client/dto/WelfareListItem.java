package com.dday.domain.welfare.client.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.Getter;
import lombok.Setter;

/**
 * 목록 응답의 {@code <servList>} 항목 하나. 필드 의미는 docs/welfare-api/NOTES.md §4-4.
 *
 * <p>API 계약 DTO가 아니라 외부 XML을 받아 담는 파싱용이다. JAXB가 필드에 직접 값을 넣으므로
 * AGENTS.md §5의 "Setter 금지"(우리 API의 Request/Response 규칙)는 여기 적용하지 않는다.
 */
@Getter
@Setter
@XmlRootElement(name = "servList")
@XmlAccessorType(XmlAccessType.FIELD)
public class WelfareListItem {

    private String servId;
    private String servNm;
    private String servDgst;
    private String servDtlLink;

    /** 생애주기. 목록 응답은 공백 없는 콤마 구분 (예: {@code 청년,중장년,노년}). */
    private String lifeArray;
    /** 대상특성. 값이 없으면 엘리먼트 자체가 없어 {@code null}로 들어온다. 값 안에 {@code ·}가 있을 수 있다. */
    private String trgterIndvdlArray;
    private String intrsThemaArray;

    private String jurMnofNm;
    private String jurOrgNm;

    private String sprtCycNm;
    private String srvPvsnNm;
    private String onapPsbltYn;
    private String inqNum;
    private String rprsCtadr;

    /** 최초등록일 {@code YYYYMMDD}. */
    private String svcfrstRegTs;

    /** 이 항목을 재직렬화한 XML. 파싱 직후 클라이언트가 채운다. 직렬화 대상에서는 제외한다. */
    @XmlTransient
    private String rawXml;
}
