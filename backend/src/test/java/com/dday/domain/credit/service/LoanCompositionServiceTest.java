package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.LoanCompositionResponse;
import com.dday.domain.credit.dto.response.LoanItemResponse;
import com.dday.domain.credit.dto.response.LoanSectorResponse;
import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.entity.FinancialSector;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoanCompositionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserAccountRepository accountRepository;

    @InjectMocks
    private LoanCompositionService loanCompositionService;

    private UserAccount account(long accountId, String orgCode, AccountType type,
                                long balance, String rate) {
        UserAccount account = UserAccount.builder()
                .orgCode(orgCode)
                .accountNum("110-0000-" + accountId)
                .accountName("계좌 " + accountId)
                .productName("상품 " + accountId)
                .accountType(type)
                .balance(balance)
                .interestRate(rate == null ? null : new BigDecimal(rate))
                .build();
        ReflectionTestUtils.setField(account, "accountId", accountId);
        return account;
    }

    private void givenAccounts(UserAccount... accounts) {
        given(accountRepository.findAllByUserUserIdOrderByAccountIdAsc(USER_ID))
                .willReturn(List.of(accounts));
    }

    @Test
    void 대출_계좌만_센다() {
        givenAccounts(
                account(1L, "088", AccountType.LOAN, 8_000_000L, "5.40"),
                account(2L, "004", AccountType.DEPOSIT, 842_000L, null),
                account(3L, "004", AccountType.SAVINGS, 3_600_000L, null));

        LoanCompositionResponse result = loanCompositionService.findComposition(USER_ID);

        assertThat(result.getLoanCount()).isEqualTo(1);
        assertThat(result.getTotalBalance()).isEqualTo(8_000_000L);
        assertThat(result.getLoans()).hasSize(1);
    }

    @Test
    void 금융권별로_묶어_건수와_잔액을_낸다() {
        givenAccounts(
                account(1L, "088", AccountType.LOAN, 8_000_000L, "5.40"),
                account(2L, "0602", AccountType.LOAN, 3_000_000L, "15.40"));

        LoanCompositionResponse result = loanCompositionService.findComposition(USER_ID);

        assertThat(result.getSectors()).extracting(LoanSectorResponse::getSector)
                .containsExactly(FinancialSector.BANK, FinancialSector.NON_BANK);
        assertThat(result.getSectors()).extracting(LoanSectorResponse::getLoanCount)
                .containsExactly(1, 1);
        assertThat(result.getSectors()).extracting(LoanSectorResponse::getTotalBalance)
                .containsExactly(8_000_000L, 3_000_000L);
        assertThat(result.getSectors().get(0).getLabel()).isEqualTo("제1금융권");
    }

    @Test
    void 기관_이름과_권역을_서버가_내려준다() {
        givenAccounts(account(1L, "0602", AccountType.LOAN, 3_000_000L, "15.40"));

        LoanCompositionResponse result = loanCompositionService.findComposition(USER_ID);

        LoanItemResponse loan = result.getLoans().get(0);
        assertThat(loan.getInstitutionName()).isEqualTo("현대캐피탈");
        assertThat(loan.getSector()).isEqualTo(FinancialSector.NON_BANK);
        assertThat(loan.getSectorLabel()).isEqualTo("제2금융권");
        assertThat(loan.getInterestRate()).isEqualByComparingTo("15.40");
    }

    @Test
    void 모르는_기관은_권역을_비우고_목록에만_남긴다() {
        // 권역을 모르는 채 제1금융권으로 넘겨짚으면 안내가 실제보다 후해진다.
        givenAccounts(account(1L, "9999", AccountType.LOAN, 2_000_000L, "19.90"));

        LoanCompositionResponse result = loanCompositionService.findComposition(USER_ID);

        assertThat(result.getLoanCount()).isEqualTo(1);
        assertThat(result.getLoans().get(0).getSector()).isNull();
        assertThat(result.getLoans().get(0).getInstitutionName()).isEqualTo("9999");
        assertThat(result.getSectors()).isEmpty();
    }

    @Test
    void 대출이_없으면_빈_응답을_준다() {
        givenAccounts(account(1L, "004", AccountType.DEPOSIT, 842_000L, null));

        LoanCompositionResponse result = loanCompositionService.findComposition(USER_ID);

        assertThat(result.getLoanCount()).isZero();
        assertThat(result.getTotalBalance()).isZero();
        assertThat(result.getLoans()).isEmpty();
        assertThat(result.getSectors()).isEmpty();
    }
}
