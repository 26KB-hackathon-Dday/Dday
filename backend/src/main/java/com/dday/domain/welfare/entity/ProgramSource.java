package com.dday.domain.welfare.entity;

/**
 * {@code welfare_program} 행이 어디서 왔는지 / 누가 소유하는지.
 */
public enum ProgramSource {

    /** 수집 배치가 외부 API에서 긁어와 룰로 통과시킨 후보. 재수집이 매번 덮어쓴다. */
    API_CANDIDATE,

    /**
     * 관리자가 손으로 교정한 행. 큐레이션 필드(카테고리·금액·지원대상·신청채널 등)를
     * 사람이 소유하므로, 재수집 시 {@code applyCollection}/{@code applyDetail}이 그 필드들을
     * 건너뛴다({@code collectedAt}·원문 XML만 갱신). 다시 {@code API_CANDIDATE}로 바꾸면 해제.
     */
    MANUAL_CURATION
}
