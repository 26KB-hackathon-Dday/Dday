package com.dday.domain.pocket.service;

import com.dday.domain.mydata.entity.*;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.request.TransactionClassificationRequest;
import com.dday.domain.pocket.dto.response.AutoClassificationResponse;
import com.dday.domain.pocket.entity.*;
import com.dday.domain.pocket.repository.*;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionClassificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private PocketRepository pocketRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserMerchantRuleRepository merchantRuleRepository;
    @Mock private TransactionClassificationHistoryRepository historyRepository;
    @InjectMocks private TransactionClassificationService service;

    @Test
    void 사용자_가맹점_규칙을_기본_자유_포켓보다_먼저_적용한다() {
        User user = org.mockito.Mockito.mock(User.class);
        Pocket free = pocket(1L, PocketType.FREE);
        Pocket essential = pocket(2L, PocketType.ESSENTIAL);
        Category food = category(10L, PocketType.ESSENTIAL, true);
        FinancialTransaction transaction = transaction("123-45-67890", "가맹점");
        UserMerchantRule rule = org.mockito.Mockito.mock(UserMerchantRule.class);
        given(rule.getPocket()).willReturn(essential);
        given(rule.getCategory()).willReturn(food);
        given(userRepository.findByUserIdAndStatus(1L, UserStatus.ACTIVE)).willReturn(Optional.of(user));
        given(pocketRepository.findByUserUserIdAndPocketType(1L, PocketType.FREE))
                .willReturn(Optional.of(free));
        given(transactionRepository.findUnclassifiedExpenses(1L)).willReturn(List.of(transaction));
        given(merchantRuleRepository.findRule(1L, "REGNO:1234567890"))
                .willReturn(Optional.of(rule));

        AutoClassificationResponse response = service.classifyUnclassified(1L);

        assertThat(response.getUserRuleCount()).isEqualTo(1);
        assertThat(response.getDefaultFreeCount()).isZero();
        assertThat(transaction.getPocket()).isSameAs(essential);
        assertThat(transaction.getCategory()).isSameAs(food);
        assertThat(transaction.getClassificationStatus()).isEqualTo(ClassificationStatus.AUTO_CLASSIFIED);
        assertThat(transaction.getClassificationSource()).isEqualTo(ClassificationSource.USER_RULE);
    }

    @Test
    void 일치하는_규칙이_없으면_카테고리를_추측하지_않고_자유_포켓으로_보낸다() {
        User user = org.mockito.Mockito.mock(User.class);
        Pocket free = pocket(1L, PocketType.FREE);
        FinancialTransaction transaction = transaction(null, "처음 보는 가맹점");
        given(userRepository.findByUserIdAndStatus(1L, UserStatus.ACTIVE)).willReturn(Optional.of(user));
        given(pocketRepository.findByUserUserIdAndPocketType(1L, PocketType.FREE))
                .willReturn(Optional.of(free));
        given(transactionRepository.findUnclassifiedExpenses(1L)).willReturn(List.of(transaction));
        given(merchantRuleRepository.findRule(1L, "NAME:처음 보는 가맹점"))
                .willReturn(Optional.empty());

        AutoClassificationResponse response = service.classifyUnclassified(1L);

        assertThat(response.getDefaultFreeCount()).isEqualTo(1);
        assertThat(transaction.getPocket()).isSameAs(free);
        assertThat(transaction.getCategory()).isNull();
        assertThat(transaction.getClassificationSource()).isEqualTo(ClassificationSource.DEFAULT_FREE);
    }

    @Test
    void 수동_분류는_상태를_보호하고_변경_이력과_향후_규칙을_저장한다() {
        User user = org.mockito.Mockito.mock(User.class);
        Pocket essential = pocket(2L, PocketType.ESSENTIAL);
        Category food = category(10L, PocketType.ESSENTIAL, true);
        FinancialTransaction transaction = transaction("1234567890", "가맹점");
        given(transactionRepository.findForClassification(1L, 100L))
                .willReturn(Optional.of(transaction));
        given(pocketRepository.findByUserUserIdAndPocketType(1L, PocketType.ESSENTIAL))
                .willReturn(Optional.of(essential));
        given(categoryRepository.findByCategoryIdAndActiveTrue(10L)).willReturn(Optional.of(food));
        given(userRepository.findByUserIdAndStatus(1L, UserStatus.ACTIVE)).willReturn(Optional.of(user));
        given(merchantRuleRepository.findRule(1L, "REGNO:1234567890"))
                .willReturn(Optional.empty());

        service.classifyManually(1L, 100L, TransactionClassificationRequest.builder()
                .pocketType(PocketType.ESSENTIAL).categoryId(10L).applyFutureRule(true).build());

        assertThat(transaction.getClassificationStatus()).isEqualTo(ClassificationStatus.MANUAL_CLASSIFIED);
        assertThat(transaction.getClassificationSource()).isEqualTo(ClassificationSource.MANUAL);
        verify(historyRepository).save(any(TransactionClassificationHistory.class));
        ArgumentCaptor<UserMerchantRule> rule = ArgumentCaptor.forClass(UserMerchantRule.class);
        verify(merchantRuleRepository).save(rule.capture());
        assertThat(rule.getValue().getMerchantKey()).isEqualTo("REGNO:1234567890");
    }

    @Test
    void 취소된_거래는_수동_분류할_수_없다() {
        User user = org.mockito.Mockito.mock(User.class);
        FinancialTransaction transaction = FinancialTransaction.builder()
                .sourceType(TransactionSourceType.ACCOUNT)
                .sourceTransactionId("TX-1")
                .transactionAt(LocalDateTime.now())
                .syncedAt(LocalDateTime.now())
                .transactionType(TransactionType.EXPENSE)
                .transactionStatus(TransactionStatus.CANCELED)
                .amount(1_000L)
                .build();
        given(userRepository.findByUserIdAndStatus(1L, UserStatus.ACTIVE)).willReturn(Optional.of(user));
        given(transactionRepository.findForClassification(1L, 100L))
                .willReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.classifyManually(1L, 100L,
                TransactionClassificationRequest.builder()
                        .pocketType(PocketType.FREE).categoryId(10L).build()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PocketErrorCode.INVALID_CLASSIFICATION_TARGET));
    }

    private FinancialTransaction transaction(String regno, String name) {
        return FinancialTransaction.builder()
                .sourceType(TransactionSourceType.ACCOUNT)
                .sourceTransactionId("TX-1")
                .transactionAt(LocalDateTime.now())
                .syncedAt(LocalDateTime.now())
                .transactionType(TransactionType.EXPENSE)
                .transactionStatus(TransactionStatus.NORMAL)
                .amount(1_000L)
                .merchantRegno(regno)
                .merchantName(name)
                .build();
    }

    private Pocket pocket(Long id, PocketType type) {
        Pocket pocket = org.mockito.Mockito.mock(Pocket.class);
        org.mockito.Mockito.lenient().when(pocket.getPocketId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(pocket.getPocketType()).thenReturn(type);
        return pocket;
    }

    private Category category(Long id, PocketType type, boolean active) {
        Category category = org.mockito.Mockito.mock(Category.class);
        org.mockito.Mockito.lenient().when(category.getCategoryId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(category.getDefaultPocketType()).thenReturn(type);
        org.mockito.Mockito.lenient().when(category.isActive()).thenReturn(active);
        return category;
    }
}
