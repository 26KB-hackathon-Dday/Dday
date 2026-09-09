package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.LoanCompositionResponse;
import com.dday.domain.credit.dto.response.LoanItemResponse;
import com.dday.domain.credit.dto.response.LoanSectorResponse;
import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.entity.FinancialSector;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 대출을 몇 건, 어느 금융권에서 받았는지 정리한다.
 *
 * <p>신용점수는 <b>얼마를 빌렸는지보다 어디서 빌렸는지</b>에 더 민감하다. 그래서 금액 합계뿐
 * 아니라 권역별로 나눠 보여준다.
 *
 * <p>인터페이스를 두지 않는다 (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class LoanCompositionService {

    private final UserAccountRepository accountRepository;

    @Transactional(readOnly = true)
    public LoanCompositionResponse findComposition(Long userId) {
        List<LoanItemResponse> loans = accountRepository
                .findAllByUserUserIdOrderByAccountIdAsc(userId).stream()
                .filter(account -> account.getAccountType() == AccountType.LOAN)
                .filter(UserAccount::isActive)
                .map(LoanItemResponse::from)
                .sorted(Comparator.comparing(LoanItemResponse::getBalance).reversed())
                .toList();
        if (loans.isEmpty()) {
            return LoanCompositionResponse.empty();
        }

        return LoanCompositionResponse.builder()
                .loanCount(loans.size())
                .totalBalance(loans.stream().mapToLong(LoanItemResponse::getBalance).sum())
                .sectors(toSectors(loans))
                .loans(loans)
                .build();
    }

    /**
     * 권역별 집계. <b>권역을 모르는 대출은 빼고 센다</b> — 모르는 기관 코드를 제1금융권으로
     * 넘겨짚으면 안내가 실제보다 후해진다. 총 건수·총 잔액에는 그대로 들어간다.
     */
    private List<LoanSectorResponse> toSectors(List<LoanItemResponse> loans) {
        Map<FinancialSector, List<LoanItemResponse>> bySector = new EnumMap<>(FinancialSector.class);
        for (LoanItemResponse loan : loans) {
            if (loan.getSector() == null) {
                continue;
            }
            bySector.computeIfAbsent(loan.getSector(), sector -> new ArrayList<>()).add(loan);
        }

        List<LoanSectorResponse> sectors = new ArrayList<>();
        for (FinancialSector sector : FinancialSector.values()) {
            List<LoanItemResponse> ofSector = bySector.get(sector);
            if (ofSector == null) {
                continue;
            }
            sectors.add(LoanSectorResponse.builder()
                    .sector(sector)
                    .label(sector.getLabel())
                    .loanCount(ofSector.size())
                    .totalBalance(ofSector.stream().mapToLong(LoanItemResponse::getBalance).sum())
                    .build());
        }
        return sectors;
    }
}
