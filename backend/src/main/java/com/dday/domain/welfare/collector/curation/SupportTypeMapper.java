package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.SupportType;

/**
 * 수집한 {@code srvPvsnNm}(급여 제공 형태)를 {@link SupportType}으로 옮긴다.
 *
 * <p>실제 값: {@code 현금지급} / {@code 현금대여(융자)} / {@code 프로그램/서비스(서비스)} /
 * {@code 이용권} / {@code 기타}, 그리고 이들의 콤마 조합.
 *
 * <p>이 결과로 상세보강 Step이 상세 API 호출 대상을 가른다 — {@link SupportType#CASH}만 부른다
 * (금액 숫자를 뽑을 수 있는 건 현금성 제도뿐).
 */
public final class SupportTypeMapper {

    private SupportTypeMapper() {
    }

    /** 콤마 조합이면 현금지급이 하나라도 있으면 CASH로 본다. 매핑 안 되면 {@code null}. */
    public static SupportType from(String srvPvsnNm) {
        if (srvPvsnNm == null || srvPvsnNm.isBlank()) {
            return null;
        }
        if (srvPvsnNm.contains("현금지급")) {
            return SupportType.CASH;
        }
        if (srvPvsnNm.contains("융자") || srvPvsnNm.contains("대여")) {
            return SupportType.LOAN;
        }
        if (srvPvsnNm.contains("이용권") || srvPvsnNm.contains("바우처")) {
            return SupportType.VOUCHER;
        }
        if (srvPvsnNm.contains("서비스") || srvPvsnNm.contains("프로그램")) {
            return SupportType.SERVICE;
        }
        return null; // 기타
    }
}
