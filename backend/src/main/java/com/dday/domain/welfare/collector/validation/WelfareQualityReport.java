package com.dday.domain.welfare.collector.validation;

import com.dday.domain.welfare.entity.WelfareProgram;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 수집분 전체를 {@link WelfareProgramValidator}로 훑은 집계.
 *
 * <p>1,000건이 들어와도 사람이 한 건씩 안 본다 — 이 리포트가 "몇 건이, 어떤 이유로"를
 * 숫자로 주고, 빈도 높은 패턴부터 대응한다. 지금은 잡 로그로만 남긴다(Step 1). 행에
 * {@code NEEDS_REVIEW}로 박고 조회 수단을 붙이는 건 다음 단계.
 */
public final class WelfareQualityReport {

    private final int total;
    private final List<String> reviewTargets;
    private final Map<WelfareIssue, List<String>> byIssue;

    private WelfareQualityReport(int total, List<String> reviewTargets,
                                 Map<WelfareIssue, List<String>> byIssue) {
        this.total = total;
        this.reviewTargets = reviewTargets;
        this.byIssue = byIssue;
    }

    public static WelfareQualityReport of(List<WelfareProgram> programs, WelfareProgramValidator validator) {
        Map<WelfareIssue, List<String>> byIssue = new LinkedHashMap<>();
        List<String> reviewTargets = new ArrayList<>();

        for (WelfareProgram p : programs) {
            List<WelfareIssue> issues = validator.validate(p);
            if (issues.isEmpty()) {
                continue;
            }
            reviewTargets.add(p.getServId());
            for (WelfareIssue issue : issues) {
                byIssue.computeIfAbsent(issue, k -> new ArrayList<>()).add(p.getServId());
            }
        }
        return new WelfareQualityReport(programs.size(), reviewTargets, byIssue);
    }

    public int total() {
        return total;
    }

    public int reviewCount() {
        return reviewTargets.size();
    }

    public int okCount() {
        return total - reviewCount();
    }

    public List<String> servIdsWith(WelfareIssue issue) {
        return byIssue.getOrDefault(issue, List.of());
    }

    /** 잡 로그용 여러 줄 문자열. */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("전체 %d건 · 정상 %d건 · 리뷰 필요 %d건".formatted(total, okCount(), reviewCount()));
        if (byIssue.isEmpty()) {
            return sb.append("\n  (이슈 없음)").toString();
        }
        byIssue.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(e -> sb.append("\n  [%2d] %-24s %s : %s".formatted(
                        e.getValue().size(),
                        e.getKey().name(),
                        e.getKey().description(),
                        String.join(", ", e.getValue()))));
        return sb.toString();
    }
}
