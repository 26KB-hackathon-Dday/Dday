package com.dday.domain.welfare.client.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 상세조회 응답 {@code <wantedDtl>} (단건).
 *
 * <p>중앙(`NationalWelfaredetailedV001`)·지자체(`LcgvWelfaredetailed`) 둘 다 루트가
 * {@code <wantedDtl>}이라 DTO·JAXB 컨텍스트를 하나만 둔다. 다만 <b>같은 뜻인데 엘리먼트명이
 * 다른 필드</b>가 있어 양쪽을 다 매핑하고 {@code getXxx()}에서 합친다:
 * <ul>
 *   <li>지원대상: 중앙 {@code tgtrDtlCn} / 지자체 {@code sprtTrgtCn}</li>
 *   <li>문의처/홈페이지 반복요소 내부: 중앙 {@code servSeDetailLink/Nm} / 지자체 {@code wlfareInfoReldCn/Nm}</li>
 * </ul>
 */
@Getter
@XmlRootElement(name = "wantedDtl")
@XmlAccessorType(XmlAccessType.FIELD)
public class WelfareDetailResponse {

    @XmlElement
    private String servId;

    @XmlElement
    private String servNm;

    /** 지원내용 — 금액·기간을 뽑는 원문. 예: {@code 매월 50만원을 지급합니다.} */
    @XmlElement
    private String alwServCn;

    /** 기준연도. 중앙만 있음. */
    @XmlElement
    private String crtrYr;

    /** 지원대상 — 중앙. */
    @XmlElement
    private String tgtrDtlCn;

    /** 지원대상 — 지자체. */
    @XmlElement
    private String sprtTrgtCn;

    /** 문의처 (반복). 첫 항목의 이름·연락처를 신청채널로 쓴다. */
    @XmlElement(name = "inqplCtadrList")
    private List<ChannelItem> inquiryContacts;

    /** 관련 홈페이지 (반복). 첫 항목의 링크를 신청채널 URL로 쓴다. */
    @XmlElement(name = "inqplHmpgReldList")
    private List<ChannelItem> relatedSites;

    @XmlElement
    private String resultCode;

    @XmlElement
    private String resultMessage;

    /** 응답 원문. 파싱 실패해도 나중에 다시 볼 수 있게 저장한다. */
    @XmlTransient
    @Setter
    private String rawXml;

    /** 문의처·관련사이트 반복요소. 중앙/지자체 내부 엘리먼트명이 달라 양쪽 다 매핑. */
    @Getter
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ChannelItem {
        @XmlElement
        private String servSeDetailLink;   // 중앙: 값(전화/URL)
        @XmlElement
        private String servSeDetailNm;     // 중앙: 이름
        @XmlElement
        private String wlfareInfoReldCn;   // 지자체: 값
        @XmlElement
        private String wlfareInfoReldNm;   // 지자체: 이름

        public String link() {
            return servSeDetailLink != null ? servSeDetailLink : wlfareInfoReldCn;
        }

        public String name() {
            return servSeDetailNm != null ? servSeDetailNm : wlfareInfoReldNm;
        }
    }

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    /** 중앙·지자체 어느 쪽이든 채워진 지원대상. */
    public String targetDescription() {
        return tgtrDtlCn != null ? tgtrDtlCn : sprtTrgtCn;
    }

    private ChannelItem firstContact() {
        return (inquiryContacts == null || inquiryContacts.isEmpty()) ? null : inquiryContacts.get(0);
    }

    private ChannelItem firstSite() {
        return (relatedSites == null || relatedSites.isEmpty()) ? null : relatedSites.get(0);
    }

    /** 첫 문의처 이름 (예: {@code 보건복지상담센터}). */
    public String channelName() {
        ChannelItem c = firstContact();
        return c == null ? null : c.name();
    }

    /** 첫 문의처 연락처 (예: {@code 129}, {@code 02-330-8680}). */
    public String channelPhone() {
        ChannelItem c = firstContact();
        return c == null ? null : c.link();
    }

    /** 첫 관련 홈페이지 (예: {@code http://www.129.go.kr}). 지자체 상세엔 없기도. */
    public String channelUrl() {
        ChannelItem s = firstSite();
        return s == null ? null : s.link();
    }
}
