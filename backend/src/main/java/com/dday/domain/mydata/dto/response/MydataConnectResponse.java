package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 마이데이터 연결 결과. 연동 완료 화면과 "모아둔 돈" 화면이 함께 쓴다.
 *
 * <p>연결과 첫 동기화가 한 번에 끝나므로 불러온 계좌를 그대로 담아 준다 —
 * 프론트가 연결 직후 계좌 목록을 또 부를 필요가 없다.
 */
@Getter
@Builder
@AllArgsConstructor
public class MydataConnectResponse {

    /** 연결된 계좌 수. 완료 화면이 "N개의 금융기관이 연결되었습니다"에 쓴다. */
    private final int connectedCount;

    private final List<ConnectedAccount> accounts;

    /** 이번에 새로 저장된 거래 수. 재연결이면 0이다(이미 있는 거래는 건너뛴다). */
    private final int insertedTransactionCount;

    public static MydataConnectResponse of(List<UserAccount> userAccounts, int insertedTransactionCount) {
        List<ConnectedAccount> accounts = userAccounts.stream()
                .map(ConnectedAccount::from)
                .toList();

        return MydataConnectResponse.builder()
                .connectedCount(accounts.size())
                .accounts(accounts)
                .insertedTransactionCount(insertedTransactionCount)
                .build();
    }

    /** 연동된 계좌 한 건. */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ConnectedAccount {

        /** 금융기관 코드. 화면이 기관 이름을 고르는 기준이다. */
        private final String institutionId;

        /** 마스킹된 계좌번호. 전체 번호를 응답에 담을 이유가 없다. */
        private final String accountNumber;

        private final Long balance;

        public static ConnectedAccount from(UserAccount account) {
            return ConnectedAccount.builder()
                    .institutionId(account.getOrgCode())
                    .accountNumber(mask(account.getAccountNum()))
                    .balance(account.getBalance())
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
}
