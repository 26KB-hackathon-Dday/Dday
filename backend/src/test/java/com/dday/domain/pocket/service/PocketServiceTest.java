package com.dday.domain.pocket.service;

import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PocketServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PocketRepository pocketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MonthlyBudgetRepository monthlyBudgetRepository;

    @Mock
    private MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private PocketService pocketService;

    /** 소유자는 이 테스트의 관심사가 아니라 비워둔다 — 조회 범위는 레포지토리 메서드가 건다. */
    private Pocket pocket(PocketType type, String name) {
        return Pocket.builder()
                .pocketType(type)
                .pocketName(name)
                .build();
    }

    @Test
    void 포켓_목록은_필수_자유_비상금_미래자산_순서로_정렬된다() {
        // DB가 돌려주는 순서를 일부러 뒤섞어 둔다. 정렬이 서비스 책임임을 확인하기 위해서다.
        given(pocketRepository.findAllByUserUserId(USER_ID)).willReturn(List.of(
                pocket(PocketType.FUTURE_ASSET, "미래자산"),
                pocket(PocketType.FREE, "자유"),
                pocket(PocketType.ESSENTIAL, "필수"),
                pocket(PocketType.EMERGENCY, "비상금")
        ));

        List<PocketResponse> result = pocketService.findAll(USER_ID);

        assertThat(result).extracting(PocketResponse::getPocketType)
                .containsExactly(PocketType.ESSENTIAL, PocketType.FREE,
                        PocketType.EMERGENCY, PocketType.FUTURE_ASSET);
    }

    @Test
    void 포켓_이름은_enum이_아니라_행에_저장된_값을_내려준다() {
        // 사용자가 포켓 이름을 바꿀 수 있어서 enum에 박지 않았다. 바뀐 이름이 그대로 나가야 한다.
        given(pocketRepository.findAllByUserUserId(USER_ID)).willReturn(List.of(
                pocket(PocketType.FREE, "용돈")
        ));

        PocketResponse result = pocketService.findAll(USER_ID).get(0);

        assertThat(result.getPocketName()).isEqualTo("용돈");
        assertThat(result.getPocketType()).isEqualTo(PocketType.FREE);
    }

    @Test
    void 없는_포켓을_조회하면_예외를_던진다() {
        given(pocketRepository.findByPocketIdAndUserUserId(999L, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.findById(USER_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("포켓을 찾을 수 없습니다.");
    }

    @Test
    void 남의_포켓을_id로_찍어도_찾을_수_없다() {
        // 소유자를 함께 걸어 조회하므로 빈 결과가 오고, 서비스는 그걸 404로 바꾼다.
        // 403으로 답하면 그 id의 포켓이 존재한다는 사실이 새어나간다.
        given(pocketRepository.findByPocketIdAndUserUserId(10L, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.findById(USER_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("포켓을 찾을 수 없습니다.");
    }
}
