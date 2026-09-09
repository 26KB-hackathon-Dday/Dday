package com.dday.domain.mydata.repository;

import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.entity.CardType;
import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.entity.TransactionSourceType;
import com.dday.domain.mydata.entity.TransactionType;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.entity.UserCard;
import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1이 없다는 걸 <b>쿼리 개수를 세서</b> 증명한다.
 *
 * <p>"돌아간다"만 확인하면 N+1은 잡히지 않는다 — 지연 로딩은 조용히 성공하기 때문이다.
 * 그래서 Hibernate {@link Statistics}로 실제 실행된 SQL 개수를 세고, <b>거래 건수를 늘려도
 * 그 값이 그대로인지</b>를 본다. 상수면 N+1이 없는 것이고, 건수를 따라 늘면 있는 것이다.
 *
 * <p>{@code @AutoConfigureTestDatabase(replace = NONE)} — 인메모리 DB로 바꾸지 않고
 * 실제 MySQL에 붙는다. 방언이 다르면 페이징이 SQL로 내려가는지를 검증할 수 없다.
 * 트랜잭션은 {@code @DataJpaTest}가 테스트마다 롤백한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FinancialTransactionRepositoryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 4, 1, 0, 0);

    @Autowired
    private FinancialTransactionRepository repository;

    @Autowired
    private EntityManager em;

    private User user;
    private UserAccount account;
    private UserCard card;
    private Category category;
    private Pocket pocket;

    @BeforeEach
    void setUp() {
        user = persist(User.builder()
                .email("n1-" + System.nanoTime() + "@dday.com")
                .passwordHash("$2a$10$" + "x".repeat(53))
                .name("테스터")
                .phone("010-0000-0000")
                .termsAgreedAt(BASE)
                .agreedLocation(false)
                .build());

        account = persist(UserAccount.builder()
                .user(user).orgCode("0004").accountNum("110-1234")
                .accountName("주계좌").accountType(AccountType.DEPOSIT).balance(1_000_000L)
                .build());

        card = persist(UserCard.builder()
                .user(user).orgCode("0301").cardIdentifier("card-1")
                .cardName("체크카드").cardType(CardType.CHECK)
                .build());

        // 코드에 유일값을 섞는다. category.uk_category_code 때문에 고정 코드를 쓰면
        // data.sql 시드(FOOD 등)와 부딪혀 테스트가 통째로 깨진다.
        category = persist(Category.builder()
                .categoryCode("TEST-" + System.nanoTime()).categoryName("테스트 카테고리")
                .defaultPocketType(PocketType.ESSENTIAL)
                .build());

        pocket = persist(Pocket.builder()
                .user(user).pocketType(PocketType.ESSENTIAL).pocketName("필수")
                .build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    /** 계좌 거래와 카드 거래를 번갈아 넣는다 — 한쪽만 넣으면 or 조건의 반쪽이 검증되지 않는다. */
    private void givenTransactions(int count) {
        for (int i = 0; i < count; i++) {
            boolean byCard = i % 2 == 1;
            FinancialTransaction tx = FinancialTransaction.builder()
                    .sourceType(byCard ? TransactionSourceType.CARD : TransactionSourceType.ACCOUNT)
                    .account(byCard ? null : account)
                    .card(byCard ? card : null)
                    .sourceTransactionId("src-" + System.nanoTime() + "-" + i)
                    .transactionAt(BASE.plusHours(i))
                    .syncedAt(BASE)
                    .transactionType(TransactionType.EXPENSE)
                    .amount(10_000L + i)
                    .merchantName("가맹점" + i)
                    .build();
            tx.classifyManually(category, pocket);
            em.persist(tx);
        }
        // 영속성 컨텍스트를 비워야 조회가 진짜 DB를 친다. 안 비우면 1차 캐시가 N+1을 가려준다.
        em.flush();
        em.clear();
    }

    private Statistics statistics() {
        Statistics stats = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return stats;
    }

    /**
     * 목록을 뽑고 <b>행마다 연관을 실제로 건드린</b> 뒤 쿼리 수를 센다.
     * 화면이 하는 일을 그대로 흉내내야 지연 로딩이 터지는지 알 수 있다.
     */
    private long queryCountForPage(int size) {
        Statistics stats = statistics();

        Page<FinancialTransaction> page = repository.findPageByUserAndPeriod(
                user.getUserId(), FROM, TO,
                PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "transactionAt")));

        for (FinancialTransaction tx : page.getContent()) {
            if (tx.getAccount() != null) {
                tx.getAccount().getAccountName();
            }
            if (tx.getCard() != null) {
                tx.getCard().getCardName();
            }
            tx.getCategory().getCategoryName();
            tx.getPocket().getPocketName();
        }
        return stats.getPrepareStatementCount();
    }

    @Test
    @DisplayName("거래가 늘어도 쿼리 개수가 그대로다 (N+1 없음)")
    void 거래가_늘어도_쿼리_개수가_그대로다() {
        givenTransactions(4);
        long few = queryCountForPage(50);

        givenTransactions(20);   // 총 24건
        long many = queryCountForPage(50);

        // N+1이 있으면 건수를 따라 늘어난다. 없으면 목록 쿼리 하나로 끝난다.
        assertThat(many).isEqualTo(few);
        assertThat(many).isEqualTo(1);
    }

    /**
     * 위 테스트가 1인 건 count 쿼리가 생략됐기 때문이다 — 첫 페이지에 결과가 다 담기면
     * Spring Data가 총 개수를 세지 않고 목록 크기로 갈음한다({@code PageableExecutionUtils}).
     * 페이지를 실제로 넘겨야 count가 나가므로, 그때도 상수(목록 1 + count 1)인지 따로 본다.
     */
    @Test
    void 페이지가_넘칠_때도_쿼리는_목록과_count_둘뿐이다() {
        givenTransactions(4);
        long few = queryCountForPage(2);

        givenTransactions(20);   // 총 24건
        long many = queryCountForPage(2);

        assertThat(many).isEqualTo(few);
        assertThat(many).isEqualTo(2);
    }

    @Test
    void 연관을_건드려도_추가_쿼리가_나가지_않는다() {
        givenTransactions(10);

        Statistics stats = statistics();
        Page<FinancialTransaction> page = repository.findPageByUserAndPeriod(
                user.getUserId(), FROM, TO, PageRequest.of(0, 10));
        long afterQuery = stats.getPrepareStatementCount();

        page.getContent().forEach(tx -> {
            tx.getCategory().getCategoryName();
            tx.getPocket().getPocketName();
        });

        // fetch join으로 이미 채워졌으므로 프록시를 건드려도 SQL이 더 나가면 안 된다.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(afterQuery);
    }

    @Test
    void 페이징은_메모리가_아니라_SQL에서_잘린다() {
        givenTransactions(10);

        Page<FinancialTransaction> page = repository.findPageByUserAndPeriod(
                user.getUserId(), FROM, TO,
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "transactionAt")));

        // to-one만 fetch하므로 조인해도 행이 곱해지지 않는다 → limit이 그대로 먹는다.
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(10);
        // 최신순 정렬도 SQL에서 이뤄진다.
        assertThat(page.getContent().get(0).getTransactionAt())
                .isAfter(page.getContent().get(2).getTransactionAt());
    }

    @Test
    void 계좌_거래와_카드_거래를_모두_가져온다() {
        givenTransactions(6);

        Page<FinancialTransaction> page = repository.findPageByUserAndPeriod(
                user.getUserId(), FROM, TO, PageRequest.of(0, 50));

        assertThat(page.getContent()).hasSize(6);
        assertThat(page.getContent()).anyMatch(t -> t.getAccount() != null);
        assertThat(page.getContent()).anyMatch(t -> t.getCard() != null);
    }

    @Test
    void 기간_밖의_거래는_빠진다() {
        givenTransactions(3);

        Page<FinancialTransaction> page = repository.findPageByUserAndPeriod(
                user.getUserId(),
                BASE.plusHours(1), BASE.plusHours(2),   // to는 미포함이라 1건만
                PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 남의_거래는_보이지_않는다() {
        givenTransactions(3);

        User other = persist(User.builder()
                .email("other-" + System.nanoTime() + "@dday.com")
                .passwordHash("$2a$10$" + "y".repeat(53))
                .name("남")
                .phone("010-1111-1111")
                .termsAgreedAt(BASE)
                .agreedLocation(false)
                .build());
        em.flush();

        Page<FinancialTransaction> page = repository.findPageByUserAndPeriod(
                other.getUserId(), FROM, TO, PageRequest.of(0, 50));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void 포켓별_조회도_쿼리가_상수다() {
        givenTransactions(12);

        Statistics stats = statistics();
        Page<FinancialTransaction> page = repository.findPageByPocketAndPeriod(
                pocket.getPocketId(), FROM, TO, PageRequest.of(0, 50));
        page.getContent().forEach(tx -> tx.getCategory().getCategoryName());

        assertThat(page.getContent()).hasSize(12);
        // 한 페이지에 다 담겨 count는 생략된다 — 목록 쿼리 하나뿐이다.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void 상세는_원거래와_상대계좌까지_한_번에_가져온다() {
        givenTransactions(1);
        Long id = repository.findPageByUserAndPeriod(user.getUserId(), FROM, TO, PageRequest.of(0, 1))
                .getContent().get(0).getFinancialTransactionId();
        em.clear();

        Statistics stats = statistics();
        FinancialTransaction tx = repository.findDetailById(id).orElseThrow();
        tx.getAccount().getAccountName();
        tx.getCategory().getCategoryName();
        tx.getPocket().getPocketName();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void 포켓_거래_조회는_소유자와_카테고리와_분류상태를_함께_검증한다() {
        givenTransactions(3);

        Page<FinancialTransaction> page = repository.findPocketPage(
                user.getUserId(),
                pocket.getPocketId(),
                FROM,
                TO,
                category.getCategoryId(),
                ClassificationStatus.MANUAL_CLASSIFIED,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "transactionAt")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allSatisfy(transaction -> {
            assertThat(transaction.getPocket().getPocketId()).isEqualTo(pocket.getPocketId());
            assertThat(transaction.getCategory().getCategoryId()).isEqualTo(category.getCategoryId());
            assertThat(transaction.getClassificationStatus())
                    .isEqualTo(ClassificationStatus.MANUAL_CLASSIFIED);
        });
    }

    @Test
    void 포켓별_사용액은_정상_소비_거래만_합산한다() {
        givenTransactions(3);

        Object[] result = repository.sumSpendingByPocket(user.getUserId(), FROM, TO).get(0);

        assertThat(result[0]).isEqualTo(pocket.getPocketId());
        assertThat(((Number) result[1]).longValue()).isEqualTo(30_003L);
    }
}
