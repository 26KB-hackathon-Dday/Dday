package com.dday.domain.housing.repository;

import com.dday.domain.housing.entity.HousingCost;
import com.dday.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @MapsId} 공유 PK가 실제로 동작하는지 확인한다. 매핑이 틀리면 컴파일은 되고
 * 저장할 때 터지므로 DB까지 가봐야 안다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HousingCostRepositoryTest {

    @Autowired
    private HousingCostRepository repository;

    @Autowired
    private EntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = newUser("hc-" + System.nanoTime() + "@dday.com");
        em.persist(user);
        em.flush();
    }

    private User newUser(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$10$" + "x".repeat(53))
                .name("테스터")
                .phone("010-0000-0000")
                .termsAgreedAt(LocalDateTime.now())
                .agreedLocation(false)
                .build();
    }

    @Test
    void PK가_회원_id와_같은_값으로_채워진다() {
        HousingCost saved = repository.save(HousingCost.builder()
                .user(user).deposit(10_000_000L).monthlyRent(450_000L).maintenanceFee(70_000L)
                .build());
        em.flush();

        // id를 직접 넣지 않았는데 @MapsId가 회원 id로 채워야 한다.
        assertThat(saved.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    void 회원_id로_바로_조회된다() {
        repository.save(HousingCost.builder()
                .user(user).deposit(10_000_000L).monthlyRent(450_000L).maintenanceFee(70_000L)
                .build());
        em.flush();
        em.clear();

        HousingCost found = repository.findById(user.getUserId()).orElseThrow();

        assertThat(found.getDeposit()).isEqualTo(10_000_000L);
        assertThat(found.estimatedMonthly()).isEqualTo(520_000L);
    }

    @Test
    void 주거비가_없는_회원은_빈_결과다() {
        assertThat(repository.findById(user.getUserId())).isEmpty();
    }

    @Test
    void 한_회원에_두_건은_저장할_수_없다() {
        repository.save(HousingCost.builder().user(user).deposit(1_000L).build());
        em.flush();
        em.clear();

        // PK가 user_id라 두 번째 행은 물리적으로 불가능하다 — 유니크 제약을 따로 걸 필요가 없다.
        // 리포지토리를 거쳐야 Hibernate 예외가 Spring 예외로 번역된다
        // (em.flush()를 직접 부르면 ConstraintViolationException이 그대로 올라온다).
        HousingCost duplicate = HousingCost.builder()
                .user(em.getReference(User.class, user.getUserId()))
                .deposit(2_000L)
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
