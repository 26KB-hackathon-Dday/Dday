package com.dday.domain.welfare.client.dto;

import com.dday.domain.welfare.entity.AgencyType;
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

    // ── LOCAL(지자체) 전용 ──────────────────────────────────────────────────
    // CENTRAL XML엔 대응 엘리먼트가 없다. 지자체 수집기가 LcgvWelfareListItem#toCommon()에서
    // 직접 채운다. @XmlTransient라 CENTRAL 파싱/재직렬화에는 영향이 없다.

    /** 운영 주체. 기본값 {@link AgencyType#CENTRAL}, 지자체 항목만 {@link AgencyType#LOCAL}. */
    @XmlTransient
    private AgencyType agencyType = AgencyType.CENTRAL;

    /** LOCAL 사업담당부서 전체 문자열. CENTRAL은 {@code null}. */
    @XmlTransient
    private String bizChrDeptNm;

    /** LOCAL 시도명. CENTRAL은 {@code null}. */
    @XmlTransient
    private String ctpvNm;

    /** LOCAL 시군구명. 광역 사업·CENTRAL은 {@code null}. */
    @XmlTransient
    private String sggNm;

    /** LOCAL 신청방법 요약 (예: {@code 방문, 전화, 우편}). CENTRAL은 {@code null}. */
    @XmlTransient
    private String aplyMtdNm;

    /** LOCAL 최종수정일 {@code YYYYMMDD}. CENTRAL은 {@code null}. */
    @XmlTransient
    private String lastModYmd;
}
