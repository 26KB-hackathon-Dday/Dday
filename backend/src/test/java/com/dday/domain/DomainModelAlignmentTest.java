package com.dday.domain;

import com.dday.domain.asset.entity.AssetType;
import com.dday.domain.asset.entity.ExecutionStatus;
import com.dday.domain.asset.entity.InvestmentActionType;
import com.dday.domain.asset.entity.InvestmentTransaction;
import com.dday.domain.budget.entity.AllocationMethod;
import com.dday.domain.budget.entity.BudgetChangeHistory;
import com.dday.domain.budget.entity.BudgetChangeType;
import com.dday.domain.budget.entity.BudgetStatus;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.mockmydata.entity.MockMydataAccount;
import com.dday.domain.mockmydata.entity.MockMydataAccountTransaction;
import com.dday.domain.mockmydata.entity.MockMydataCardTransaction;
import com.dday.domain.mydata.entity.ClassificationSource;
import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.entity.TransactionSourceType;
import com.dday.domain.mydata.entity.TransactionStatus;
import com.dday.domain.mydata.entity.TransactionType;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.pocket.entity.ClassificationChangedBy;
import com.dday.domain.pocket.entity.PocketType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelAlignmentTest {

    @Test
    void 핵심_도메인_enum은_DB_계약의_값과_순서를_유지한다() {
        assertThat(PocketType.values()).extracting(Enum::name)
                .containsExactly("ESSENTIAL", "FREE", "EMERGENCY", "FUTURE_ASSET");
        assertThat(TransactionType.values()).extracting(Enum::name)
                .containsExactly("INCOME", "EXPENSE", "SELF_TRANSFER", "OTHER");
        assertThat(TransactionStatus.values()).extracting(Enum::name)
                .containsExactly("NORMAL", "CANCELED", "REFUNDED");
        assertThat(TransactionSourceType.values()).extracting(Enum::name)
                .containsExactly("ACCOUNT", "CARD");
        assertThat(ClassificationStatus.values()).extracting(Enum::name)
                .containsExactly("AUTO_CLASSIFIED", "MANUAL_CLASSIFIED", "UNCLASSIFIED");
        assertThat(ClassificationSource.values()).extracting(Enum::name)
                .containsExactly("USER_RULE", "MYDATA_CATEGORY", "MERCHANT_DB",
                        "MERCHANT_NAME_RULE", "MANUAL", "DEFAULT_FREE");
        assertThat(ClassificationChangedBy.values()).extracting(Enum::name)
                .containsExactly("USER", "SYSTEM");
    }

    @Test
    void 예산과_자산_enum은_DB_계약의_값과_순서를_유지한다() {
        assertThat(BudgetStatus.values()).extracting(Enum::name)
                .containsExactly("DRAFT", "CONFIRMED");
        assertThat(BudgetChangeType.values()).extracting(Enum::name)
                .containsExactly("USER_EDIT", "REALLOCATION");
        assertThat(AllocationMethod.values()).extracting(Enum::name)
                .containsExactly("USER_INPUT", "AUTO_REMAINDER", "SYSTEM_DRAFT");
        assertThat(AssetType.values()).extracting(Enum::name)
                .containsExactly("STOCK", "ETF", "FUND", "SAVINGS", "ETC");
        assertThat(ExecutionStatus.values()).extracting(Enum::name)
                .containsExactly("EXECUTED", "CANCELED", "CORRECTED");
        assertThat(InvestmentActionType.values()).extracting(Enum::name)
                .containsExactly("BUY", "SELL", "DEPOSIT", "WITHDRAW");
    }

    @Test
    void 원_단위_금액은_Long을_사용한다() throws Exception {
        assertFieldType(UserAccount.class, "balance", Long.class);
        assertFieldType(UserAccount.class, "availableBalance", Long.class);
        assertFieldType(MockMydataAccount.class, "balance", Long.class);
        assertFieldType(MockMydataAccount.class, "availableBalance", Long.class);
        assertFieldType(MockMydataAccountTransaction.class, "amount", Long.class);
        assertFieldType(MockMydataCardTransaction.class, "amount", Long.class);
        assertFieldType(FinancialTransaction.class, "amount", Long.class);
        assertFieldType(MonthlyBudget.class, "totalBudgetAmount", Long.class);
        assertFieldType(MonthlyPocketBudget.class, "targetAmount", Long.class);
        assertFieldType(BudgetChangeHistory.class, "previousTotalBudget", Long.class);
        assertFieldType(BudgetChangeHistory.class, "changedTotalBudget", Long.class);
        assertFieldType(InvestmentTransaction.class, "amount", Long.class);
    }

    @Test
    void 금액_컬럼은_DB_계약의_검사_제약을_유지한다() {
        assertCheck(MonthlyBudget.class, "chk_total_budget_amount", "total_budget_amount > 0");
        assertCheck(MonthlyPocketBudget.class, "chk_pocket_target_amount", "target_amount >= 0");
        assertCheck(FinancialTransaction.class, "chk_transaction_amount", "amount >= 0");
        assertCheck(InvestmentTransaction.class, "chk_investment_amount", "amount >= 0");
    }

    @Test
    void enum_필드는_문자열로_저장한다() throws Exception {
        assertStringEnum(FinancialTransaction.class, "sourceType");
        assertStringEnum(FinancialTransaction.class, "transactionType");
        assertStringEnum(FinancialTransaction.class, "transactionStatus");
        assertStringEnum(FinancialTransaction.class, "classificationStatus");
        assertStringEnum(FinancialTransaction.class, "classificationSource");
        assertStringEnum(MonthlyBudget.class, "budgetStatus");
        assertStringEnum(MonthlyPocketBudget.class, "allocationMethod");
        assertStringEnum(InvestmentTransaction.class, "assetType");
        assertStringEnum(InvestmentTransaction.class, "actionType");
        assertStringEnum(InvestmentTransaction.class, "executionStatus");
    }

    @Test
    void 연관관계는_지연_로딩한다() throws Exception {
        assertLazyManyToOne(UserAccount.class, "user");
        assertLazyManyToOne(FinancialTransaction.class, "account");
        assertLazyManyToOne(FinancialTransaction.class, "card");
        assertLazyManyToOne(FinancialTransaction.class, "counterpartyAccount");
        assertLazyManyToOne(FinancialTransaction.class, "originalTransaction");
        assertLazyManyToOne(FinancialTransaction.class, "category");
        assertLazyManyToOne(FinancialTransaction.class, "pocket");
        assertLazyManyToOne(MonthlyBudget.class, "user");
        assertLazyManyToOne(MonthlyPocketBudget.class, "monthlyBudget");
        assertLazyManyToOne(MonthlyPocketBudget.class, "pocket");
        assertLazyManyToOne(InvestmentTransaction.class, "account");
    }

    private void assertFieldType(Class<?> owner, String fieldName, Class<?> expectedType)
            throws NoSuchFieldException {
        assertThat(owner.getDeclaredField(fieldName).getType()).isEqualTo(expectedType);
    }

    private void assertStringEnum(Class<?> owner, String fieldName) throws NoSuchFieldException {
        Enumerated annotation = owner.getDeclaredField(fieldName).getAnnotation(Enumerated.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(EnumType.STRING);
    }

    private void assertCheck(Class<?> owner, String name, String constraints) {
        Check annotation = owner.getAnnotation(Check.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo(name);
        assertThat(annotation.constraints()).isEqualTo(constraints);
    }

    private void assertLazyManyToOne(Class<?> owner, String fieldName) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(fieldName);
        ManyToOne annotation = field.getAnnotation(ManyToOne.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.fetch()).isEqualTo(FetchType.LAZY);
    }
}
