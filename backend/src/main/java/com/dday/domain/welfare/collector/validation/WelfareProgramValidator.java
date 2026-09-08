package com.dday.domain.welfare.collector.validation;

import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 수집·보강이 끝난 {@link WelfareProgram} 한 건이 화면에 내보내도 되는 상태인지 점검한다.
 *
 * <p>인스턴스별로 대응(레코드마다 규칙 추가)하지 않기 위한 장치다. 여기서 잡힌 이슈를
 * 집계({@link WelfareQualityReport})해 "18건 중 몇 건이, 어떤 이유로" 리뷰가 필요한지
 * 숫자로 본다. 값을 고치지는 않는다.
 */
@Component
public class WelfareProgramValidator {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("10000");
    private static final BigDecimal MAX_MONTHLY = new BigDecimal("10000000");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("200000000");

    /**
     * 자격요건이 아니라 접수 안내·공고문일 때의 신호. {@code ※}는 부연·주의 마커라 첫머리에 오면
     * 지원대상 서술이 아닐 공산이 크다. {@code ○}·{@code -} 같은 일반 불릿은 정상 내용도 자주
     * 그렇게 시작하므로 마커만으로는 안 잡고, 앞부분에 접수기간류 단어가 있을 때만 잡는다.
     */
    private static final Pattern NOTICE_LEAD = Pattern.compile("^※");
    private static final Pattern NOTICE_WORDS =
            Pattern.compile("신청\\s*기간|접수\\s*기간|신청\\s*접수|공고|모집\\s*기간|선정자\\s*공지");

    public List<WelfareIssue> validate(WelfareProgram p) {
        List<WelfareIssue> issues = new ArrayList<>();

        if (isBlank(p.getCategory())) {
            issues.add(WelfareIssue.CATEGORY_MISSING);
        }

        if (p.getSupportType() == SupportType.CASH && p.getSupportAmount() == null) {
            issues.add(WelfareIssue.CASH_WITHOUT_AMOUNT);
        }
        if (p.getSupportAmount() != null && amountLooksWrong(p.getSupportAmount(), p.getSupportAmountType())) {
            issues.add(WelfareIssue.AMOUNT_SUSPICIOUS);
        }

        String target = p.getTargetDescription();
        if (isBlank(target)) {
            issues.add(WelfareIssue.TARGET_MISSING);
        } else if (looksLikeNotice(target)) {
            issues.add(WelfareIssue.TARGET_LOOKS_LIKE_NOTICE);
        }

        if (isBlank(p.getApplyChannelName())
                && isBlank(p.getApplyChannelUrl())
                && isBlank(p.getApplyChannelPhone())) {
            issues.add(WelfareIssue.CHANNEL_MISSING);
        }
        // URL 스킴 누락은 검증 대상이 아니다 — DetailText.normalizeUrl이 수집·응답 양쪽에서
        // 자동 교정하므로 "사람이 봐야 하는 것"이 아니라 노이즈다.

        return issues;
    }

    /** 이슈가 하나라도 있으면 리뷰 대상. */
    public boolean needsReview(WelfareProgram p) {
        return !validate(p).isEmpty();
    }

    private static boolean amountLooksWrong(BigDecimal amount, SupportAmountType type) {
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            return true;
        }
        BigDecimal ceiling = type == SupportAmountType.MONTHLY ? MAX_MONTHLY : MAX_TOTAL;
        return amount.compareTo(ceiling) > 0;
    }

    private static boolean looksLikeNotice(String target) {
        String head = target.strip();
        if (NOTICE_LEAD.matcher(head).find()) {
            return true;
        }
        String first = head.length() > 40 ? head.substring(0, 40) : head;
        return NOTICE_WORDS.matcher(first).find();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 리포트에서 쓰기 좋게 전체 이슈 종류를 노출. */
    public Set<WelfareIssue> allIssueTypes() {
        return EnumSet.allOf(WelfareIssue.class);
    }
}
