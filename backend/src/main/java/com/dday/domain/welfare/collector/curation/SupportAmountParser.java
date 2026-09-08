package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.SupportAmountType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 상세 API의 {@code alwServCn}(지원내용) 자연어에서 지원 <b>금액</b>과 <b>지원 개월 수</b>를
 * best-effort로 뽑는다.
 *
 * <pre>
 * 자립수당      "매월 50만원을 지급합니다"                       → 500000, null
 * 사회첫걸음    "월 300,000원, 최대 60개월 지급"                 → 300000, 60
 * 청년월세      "최대 480만원(월 최대 20만원)까지 24개월"        → 200000, 24  (MONTHLY면 월 금액 우선)
 * 근로장학금    "시간당 10,320원"                               → null        (시간당은 지원액 아님)
 * </pre>
 *
 * <p>완벽하지 않다 — 여러 제도를 한 필드에 뭉쳐 쓰거나(해외취업), 본인 납입액을 지원액처럼
 * 적은 경우(내일저축계좌)는 틀린다. 최종 검증은 관리자 몫.
 */
public final class SupportAmountParser {

    /** "월 최대 20만원", "매월 50만 원" — 월별 지급액을 콕 집는다. */
    private static final Pattern MONTHLY_AMOUNT =
            Pattern.compile("(?:월|매월)\\s*(?:최대\\s*)?([0-9][0-9,]*)\\s*(만)?\\s*원");
    /** "50만원", "1,200 만원". */
    private static final Pattern MAN_WON = Pattern.compile("([0-9][0-9,]*)\\s*만\\s*원");
    /** "300,000원". */
    private static final Pattern WON = Pattern.compile("([0-9][0-9,]{2,})\\s*원");
    /** "시간당 10,320원", "시급" — 지원 총액이 아니다. */
    private static final Pattern HOURLY = Pattern.compile("시간당|시급");
    private static final Pattern MONTHS = Pattern.compile("([0-9]+)\\s*개월");
    private static final Pattern YEARS = Pattern.compile("([0-9]+)\\s*년");

    private SupportAmountParser() {
    }

    public record Parsed(Long amount, Integer months) {
    }

    public static Parsed parse(String alwServCn, SupportAmountType type) {
        if (alwServCn == null || alwServCn.isBlank()) {
            return new Parsed(null, null);
        }
        return new Parsed(amount(alwServCn, type), months(alwServCn));
    }

    private static Long amount(String text, SupportAmountType type) {
        // 시간당 표기만 있으면(근로장학금 등) 지원액을 못 뽑는다
        if (HOURLY.matcher(text).find() && !MONTHLY_AMOUNT.matcher(text).find()) {
            return null;
        }
        // 월별 지급이면 "월 N원"을 먼저 본다 (총액이 아니라 월 금액이 필요)
        if (type == SupportAmountType.MONTHLY) {
            Matcher m = MONTHLY_AMOUNT.matcher(text);
            if (m.find()) {
                return scale(m.group(1), m.group(2) != null);
            }
        }
        Matcher man = MAN_WON.matcher(text);
        if (man.find()) {
            return scale(man.group(1), true);
        }
        Matcher won = WON.matcher(text);
        if (won.find()) {
            return scale(won.group(1), false);
        }
        return null;
    }

    private static Integer months(String text) {
        Matcher m = MONTHS.matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        Matcher y = YEARS.matcher(text);
        if (y.find()) {
            return Integer.parseInt(y.group(1)) * 12;
        }
        return null;
    }

    /**
     * 원 단위 정수로 만든다. 자릿수가 비정상적으로 큰 텍스트(오타·잡탕 문장)는
     * {@code long}을 넘겨 음수로 뒤집힐 수 있어 파싱 단계에서 버린다 —
     * 이상값은 {@code WelfareProgramValidator}가 다시 걸러 리뷰 큐로 보낸다.
     */
    private static Long scale(String digitsWithCommas, boolean isManUnit) {
        try {
            long n = Long.parseLong(digitsWithCommas.replace(",", ""));
            return isManUnit ? Math.multiplyExact(n, 10_000L) : n;
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
