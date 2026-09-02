package com.dday.domain.welfare.collector;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.collector.rule.Rule1StrongKeyword;
import com.dday.domain.welfare.collector.rule.Rule2JurOrgName;
import com.dday.domain.welfare.collector.rule.Rule3LifeStage;
import com.dday.domain.welfare.collector.rule.Rule4TargetCount;
import com.dday.domain.welfare.collector.rule.Rule5DgstPosition;
import com.dday.domain.welfare.collector.rule.RuleHit;
import com.dday.domain.welfare.collector.rule.YouthPreFilter;
import com.dday.domain.welfare.entity.YouthStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록 항목 하나가 청년 대상인지 판정한다. 룰의 근거는 docs/welfare-api/NOTES.md §7,
 * 이 조합이 실데이터 10건에서 어떻게 나오는지는 같은 문서 §7-4.
 *
 * <pre>
 *   사전필터 탈락            → DISCARDED
 *   Rule 1 키워드 매칭        → STORED / STRONG_YOUTH
 *   스코어 ≥ 3               → STORED / AUTO_APPROVED
 *   스코어 0~2              → STORED / REVIEW_QUEUE
 *   스코어 < 0              → REJECTED
 * </pre>
 *
 * 상태를 갖지 않는다 — 순수 함수다.
 */
@Component
public class YouthClassifier {

    private static final int AUTO_APPROVE_THRESHOLD = 3;

    public Classification classify(WelfareListItem item) {
        String servNm = item.getServNm();
        String servDgst = item.getServDgst();
        String jurOrgNm = item.getJurOrgNm();

        if (!YouthPreFilter.passes(servNm, servDgst, jurOrgNm)) {
            return Classification.discarded();
        }
        if (Rule1StrongKeyword.matches(servNm, servDgst)) {
            return Classification.strongYouth();
        }

        List<RuleHit> hits = List.of(
                Rule2JurOrgName.apply(jurOrgNm),
                Rule3LifeStage.apply(item.getLifeArray()),
                Rule4TargetCount.apply(item.getTrgterIndvdlArray()),
                Rule5DgstPosition.apply(servDgst)
        );

        int score = 0;
        List<String> trace = new ArrayList<>();
        for (RuleHit hit : hits) {
            if (hit.scored()) {
                score += hit.delta();
                trace.add(hit.tag());
            }
        }
        String traceStr = String.join(",", trace) + "=" + score;

        if (score < 0) {
            return Classification.rejected(score, traceStr);
        }
        YouthStatus status = score >= AUTO_APPROVE_THRESHOLD
                ? YouthStatus.AUTO_APPROVED
                : YouthStatus.REVIEW_QUEUE;
        return Classification.scored(status, score, traceStr);
    }
}
