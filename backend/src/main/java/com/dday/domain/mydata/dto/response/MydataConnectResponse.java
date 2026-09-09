package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.entity.Institution;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.entity.UserCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 마이데이터 연결 결과. 연동 완료 화면과 "모아둔 돈" 화면이 함께 쓴다.
 *
 * <p>연결과 첫 동기화가 한 번에 끝나므로 불러온 계좌·카드를 그대로 담아 준다 —
 * 프론트가 연결 직후 목록을 또 부를 필요가 없다.
 */
@Getter
@Builder
@AllArgsConstructor
public class MydataConnectResponse {

    /**
     * 연결된 <b>기관</b> 수 — 계좌·카드 행 수가 아니다. 완료 화면이
     * "N개의 금융기관이 연결되었습니다"에 쓰는데, 국민은행 계좌가 3개라고 3개 기관은
     * 아니다. {@link #institutions}의 크기와 같다.
     */
    private final int connectedCount;

    private final List<ConnectedAccount> accounts;

    /** 연결된 기관 목록(계좌·카드 통틀어 중복 제거). 완료 화면의 체크리스트가 이걸 그린다. */
    private final List<ConnectedInstitution> institutions;

    /** 이번에 새로 저장된 거래 수. 재연결이면 0이다(이미 있는 거래는 건너뛴다). */
    private final int insertedTransactionCount;

    public static MydataConnectResponse of(List<UserAccount> userAccounts, List<UserCard> userCards,
                                           int insertedTransactionCount) {
        List<ConnectedAccount> accounts = userAccounts.stream()
                .map(ConnectedAccount::from)
                .toList();

        // orgCode로 중복을 걸러내되, 처음 등장한 순서(계좌 먼저, 카드는 그 다음)를 유지한다.
        Map<String, ConnectedInstitution> institutionsByOrgCode = new LinkedHashMap<>();
        userAccounts.forEach(account -> institutionsByOrgCode
                .computeIfAbsent(account.getOrgCode(), ConnectedInstitution::of));
        userCards.forEach(card -> institutionsByOrgCode
                .computeIfAbsent(card.getOrgCode(), ConnectedInstitution::of));
        List<ConnectedInstitution> institutions = List.copyOf(institutionsByOrgCode.values());

        return MydataConnectResponse.builder()
                .connectedCount(institutions.size())
                .accounts(accounts)
                .institutions(institutions)
                .insertedTransactionCount(insertedTransactionCount)
                .build();
    }

    /** 연동된 계좌 한 건. */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ConnectedAccount {

        /** 금융기관 코드(예: 004). 화면 표시가 아니라 식별용이다. */
        private final String institutionId;

        /**
         * 화면에 그대로 띄우는 기관 이름(예: 국민은행).
         *
         * <p>프론트가 코드를 이름으로 바꾸지 않도록 서버가 내려준다 — 각 화면이 매핑표를
         * 따로 들면 어디서는 "국민은행", 어디서는 "004"가 뜬다.
         */
        private final String institutionName;

        /** 마스킹된 계좌번호. 전체 번호를 응답에 담을 이유가 없다. */
        private final String accountNumber;

        private final Long balance;

        /**
         * DEPOSIT · SAVINGS · LOAN · INVESTMENT · ETC. "모아둔 자산" 화면이 대출 잔액을
         * 자산으로 합산하지 않으려면 이 값으로 걸러야 한다 — 대출은 빚이지 자산이 아니다.
         */
        private final AccountType accountType;

        public static ConnectedAccount from(UserAccount account) {
            return ConnectedAccount.builder()
                    .institutionId(account.getOrgCode())
                    .institutionName(Institution.nameOf(account.getOrgCode()))
                    .accountNumber(mask(account.getAccountNum()))
                    .balance(account.getBalance())
                    .accountType(account.getAccountType())
                    .build();
        }

        /**
         * 뒤 4자리만 남기고 가린다. 짧아서 가릴 게 없으면 그대로 둔다 —
         * 억지로 자르면 화면에 의미 없는 문자열이 뜬다.
         */
        private static String mask(String accountNum) {
            if (accountNum == null || accountNum.length() <= 4) {
                return accountNum;
            }
            return "****" + accountNum.substring(accountNum.length() - 4);
        }
    }

    /** 연동된 기관 하나(계좌든 카드든). 완료 화면이 "무엇을 연결했는지"만 보여줄 때 쓴다. */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ConnectedInstitution {

        private final String institutionId;
        private final String institutionName;

        static ConnectedInstitution of(String orgCode) {
            return ConnectedInstitution.builder()
                    .institutionId(orgCode)
                    .institutionName(Institution.nameOf(orgCode))
                    .build();
        }
    }
}
