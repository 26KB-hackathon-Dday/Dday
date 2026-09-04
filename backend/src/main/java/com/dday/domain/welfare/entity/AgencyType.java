package com.dday.domain.welfare.entity;

/**
 * 복지서비스를 운영하는 주체 구분.
 *
 * <ul>
 *   <li>{@link #CENTRAL} — 중앙부처복지서비스. 전국 시행이라 지역 필드가 없다.</li>
 *   <li>{@link #LOCAL} — 지자체복지서비스. {@code ctpvNm}/{@code sggNm}(지역),
 *       {@code lastModYmd}(최종수정일)가 추가로 있다 — docs/welfare-api/NOTES.md §11.</li>
 * </ul>
 *
 * <p>두 소스는 응답 스키마가 달라 목록 Step을 따로 두지만({@code collectYouthListStep} /
 * {@code collectLocalListStep}), 변환 후에는 같은 판정·저장 파이프라인을 탄다.
 */
public enum AgencyType {
    CENTRAL,
    LOCAL
}
