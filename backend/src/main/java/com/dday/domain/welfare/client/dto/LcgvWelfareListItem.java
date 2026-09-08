package com.dday.domain.welfare.client.dto;

import com.dday.domain.welfare.entity.AgencyType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.Getter;
import lombok.Setter;

/**
 * 지자체복지서비스 목록 응답의 {@code <servList>} 항목 하나. 필드 대조는 docs/welfare-api/NOTES.md §11-3.
 *
 * <p>CENTRAL({@link WelfareListItem})과 겹치는 필드가 많지만 엘리먼트명이 다르다
 * ({@code lifeNmArray} vs {@code lifeArray}, {@code intrsThemaNmArray} vs {@code intrsThemaArray}),
 * 소관이 {@code bizChrDeptNm} 한 필드로 합쳐져 있고, 지역({@code ctpvNm}/{@code sggNm})과
 * 최종수정일({@code lastModYmd})이 추가로 있다.
 *
 * <p>판정·저장 파이프라인은 CENTRAL {@link WelfareListItem}에 맞춰져 있으므로,
 * {@link #toCommon()}으로 변환해서 흘려보낸다.
 */
@Getter
@Setter
@XmlRootElement(name = "servList")
@XmlAccessorType(XmlAccessType.FIELD)
public class LcgvWelfareListItem {

    private String servId;
    private String servNm;
    private String servDgst;
    private String servDtlLink;

    /** 생애주기. LOCAL은 {@code "청소년, 청년"}처럼 콤마 뒤 공백이 붙는다. */
    private String lifeNmArray;
    /** 관심주제. LOCAL은 {@code "보호·돌봄, 서민금융"}처럼 콤마 뒤 공백이 붙는다. */
    private String intrsThemaNmArray;

    /** 사업담당부서 전체 문자열 (예: {@code 서울특별시 용산구 생활지원국 아동청소년과}). */
    private String bizChrDeptNm;

    /** 시도명 (예: {@code 서울특별시}). */
    private String ctpvNm;
    /** 시군구명 (예: {@code 용산구}). 광역(시도) 단위 사업이면 엘리먼트 자체가 없다 → {@code null}. */
    private String sggNm;

    private String aplyMtdNm;
    /** 최종수정일 {@code YYYYMMDD}. 종료 감지(후속)용. */
    private String lastModYmd;

    private String sprtCycNm;
    private String srvPvsnNm;
    private String inqNum;

    /** 이 항목을 재직렬화한 XML. 파싱 직후 클라이언트가 채운다. */
    @XmlTransient
    private String rawXml;

    /**
     * 공통 파이프라인용 {@link WelfareListItem}으로 변환한다.
     *
     * <ul>
     *   <li>{@code lifeNmArray}/{@code intrsThemaNmArray}의 콤마 뒤 공백을 제거해 CENTRAL 포맷에 맞춘다
     *       (Rule 3의 split은 trim을 하지만 저장 문자열을 CENTRAL과 통일해 둔다)</li>
     *   <li>{@code bizChrDeptNm}을 {@code jurOrgNm} 자리에도 넣는다 — Rule 2가 여기서 "청년"을 찾는다</li>
     *   <li>{@code trgterIndvdlArray}는 LOCAL에 없으므로 {@code null} (Rule 4는 중립)</li>
     * </ul>
     */
    public WelfareListItem toCommon() {
        WelfareListItem common = new WelfareListItem();
        common.setAgencyType(AgencyType.LOCAL);
        common.setServId(servId);
        common.setServNm(servNm);
        common.setServDgst(servDgst);
        common.setServDtlLink(servDtlLink);
        common.setLifeArray(stripCommaSpace(lifeNmArray));
        common.setIntrsThemaArray(stripCommaSpace(intrsThemaNmArray));
        common.setJurOrgNm(bizChrDeptNm);
        common.setSprtCycNm(sprtCycNm);
        common.setSrvPvsnNm(srvPvsnNm);
        common.setInqNum(inqNum);
        common.setRawXml(rawXml);
        common.setBizChrDeptNm(bizChrDeptNm);
        common.setCtpvNm(ctpvNm);
        common.setSggNm(sggNm);
        common.setAplyMtdNm(aplyMtdNm);
        common.setLastModYmd(lastModYmd);
        return common;
    }

    private static String stripCommaSpace(String value) {
        return value == null ? null : value.replace(", ", ",");
    }
}
