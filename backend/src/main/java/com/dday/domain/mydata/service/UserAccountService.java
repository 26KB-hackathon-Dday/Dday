package com.dday.domain.mydata.service;

import com.dday.domain.mydata.dto.MydataErrorCode;
import com.dday.domain.mydata.dto.response.UserAccountListResponse;
import com.dday.domain.mydata.dto.response.UserAccountResponse;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.repository.UserAccountRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마이페이지의 금융정보 관리 화면. 연동된 계좌 목록을 보여주고, 예산 계산에 넣을지를
 * 사용자가 고르게 한다.
 *
 * <p>계좌를 추가·해지하는 건 여기서 하지 않는다 — 그건 MyData 쪽(연동·동기화)의 몫이고,
 * 여기는 이미 동기화된 계좌의 {@code selected}만 다룬다.
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final FinancialTransactionRepository financialTransactionRepository;

    @Transactional(readOnly = true)
    public UserAccountListResponse getAccounts(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        Map<Long, Long> monthlyContributions = financialTransactionRepository
                .sumMonthlyFutureAssetContributionByAccount(
                        userId,
                        currentMonth.atDay(1).atStartOfDay(),
                        currentMonth.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
        return UserAccountListResponse.of(
                userAccountRepository.findAllByUserUserIdOrderByAccountIdAsc(userId),
                monthlyContributions);
    }

    /**
     * <b>조회 단계에서 소유자를 함께 건다</b> — id만으로 찾아 뒤에서 검사하면
     * 검사를 빠뜨린 코드가 남의 계좌를 건드린다.
     */
    @Transactional
    public UserAccountResponse updateSelection(Long userId, Long accountId, boolean selected) {
        UserAccount account = userAccountRepository.findByAccountIdAndUserUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException(MydataErrorCode.ACCOUNT_NOT_FOUND));

        account.select(selected);
        return UserAccountResponse.from(account);
    }
}
