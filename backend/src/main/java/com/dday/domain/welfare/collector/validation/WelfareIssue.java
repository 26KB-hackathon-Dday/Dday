package com.dday.domain.welfare.collector.validation;

/**
 * 큐레이션 품질 점검 항목. 한 행이 여러 개를 동시에 가질 수 있다.
 *
 * <p>여기 잡히는 건 "이 값이 이상하다"는 <b>탐지</b>지 자동 교정이 아니다. 탐지 규칙은
 * 오탐이 좀 있어도 된다 — 사람이 리뷰 큐에서 확인하니까. 값을 바꾸는 로직에 휴리스틱을
 * 넣는 것과는 위치가 다르다.
 */
public enum WelfareIssue {

    CATEGORY_MISSING("카테고리 미분류 (관심주제·제도명으로 못 가림)"),
    CASH_WITHOUT_AMOUNT("현금성(CASH) 제도인데 금액 파싱 실패"),
    AMOUNT_SUSPICIOUS("지원 금액이 비현실적 (월 1천만원 초과 / 총 2억 초과 / 1만원 미만)"),
    TARGET_MISSING("지원대상 서술 없음 (상세 API에 tgtrDtlCn·sprtTrgtCn 모두 비어 있음)"),
    TARGET_LOOKS_LIKE_NOTICE("지원대상이 자격요건이 아니라 안내문·공고문 형태"),
    CHANNEL_MISSING("신청채널 정보 전무 (이름·URL·전화 모두 없음)");

    private final String description;

    WelfareIssue(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
