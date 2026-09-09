package com.dday.domain.mydata.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 금융기관 코드 ↔ 화면에 띄우는 이름.
 *
 * <p><b>이름을 프론트에서 만들지 않는다.</b> 화면마다 매핑표를 따로 두면 "004"가 어디서는
 * 국민은행, 어디서는 코드 그대로 뜬다 (실제로 그랬다). 표시용 문구의 정본은 서버다 —
 * AGENTS.md §2와 같은 이유다.
 *
 * <p>코드는 은행권 표준 기관코드를 따른다. 카드사 코드는 목데이터용 임의값이라
 * 실제 마이데이터 기관코드와 다를 수 있다 ({@code data.sql} 주석 참고).
 *
 * <p>enum 이름이 아니라 {@link #code}로 찾는다. DB에는 코드가 들어 있고, 모르는 코드가
 * 와도 화면이 죽으면 안 되기 때문에 {@link #nameOf}는 코드를 그대로 돌려준다.
 */
@Getter
@RequiredArgsConstructor
public enum Institution {

    KOOKMIN("004", "국민은행", FinancialSector.BANK),
    SHINHAN("088", "신한은행", FinancialSector.BANK),
    WOORI("020", "우리은행", FinancialSector.BANK),
    HANA("081", "하나은행", FinancialSector.BANK),
    NONGHYUP("011", "농협은행", FinancialSector.BANK),
    KAKAO("090", "카카오뱅크", FinancialSector.BANK),
    TOSS("092", "토스뱅크", FinancialSector.BANK),

    HYUNDAI_CARD("0302", "현대카드", FinancialSector.NON_BANK),
    SAMSUNG_CARD("0303", "삼성카드", FinancialSector.NON_BANK),
    KB_CARD("0301", "KB국민카드", FinancialSector.NON_BANK),
    SHINHAN_CARD("0306", "신한카드", FinancialSector.NON_BANK),
    WOORI_CARD("0313", "우리카드", FinancialSector.NON_BANK),

    HYUNDAI_CAPITAL("0602", "현대캐피탈", FinancialSector.NON_BANK),
    SBI_SAVINGS("0720", "SBI저축은행", FinancialSector.NON_BANK);

    private final String code;
    private final String institutionName;

    /** 이 기관이 속한 권역. 대출을 어디서 받았는지 구분하는 데 쓴다. */
    private final FinancialSector sector;

    /**
     * 코드에 해당하는 권역. <b>모르는 코드면 {@code null}이다.</b>
     * 권역을 모르는 채 제1금융권으로 넘겨짚으면 점수 안내가 실제보다 후하게 나간다.
     */
    public static FinancialSector sectorOf(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(institution -> institution.code.equals(code))
                .map(Institution::getSector)
                .findFirst()
                .orElse(null);
    }

    /**
     * 코드에 해당하는 기관 이름. <b>모르는 코드면 코드를 그대로 돌려준다.</b>
     * 목록에 없는 기관이 들어왔다고 화면이 비거나 예외가 나는 것보다, 코드라도 보이는 게 낫다.
     */
    public static String nameOf(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(institution -> institution.code.equals(code))
                .map(Institution::getInstitutionName)
                .findFirst()
                .orElse(code);
    }
}
