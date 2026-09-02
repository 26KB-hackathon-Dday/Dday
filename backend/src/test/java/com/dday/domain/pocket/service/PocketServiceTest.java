package com.dday.domain.pocket.service;

import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PocketServiceTest {

    @Mock
    private PocketRepository pocketRepository;

    @InjectMocks
    private PocketService pocketService;

    private Pocket pocket(PocketType type, String budget, String spent) {
        return Pocket.builder()
                .type(type)
                .monthlyBudget(new BigDecimal(budget))
                .spentThisMonth(new BigDecimal(spent))
                .build();
    }

    @Test
    void 포켓_목록은_주거_생활_비상_자산형성_순서로_정렬된다() {
        // DB가 돌려주는 순서를 일부러 뒤섞어 둔다. 정렬이 서비스 책임임을 확인하기 위해서다.
        given(pocketRepository.findAll()).willReturn(List.of(
                pocket(PocketType.ASSET, "100000", "0"),
                pocket(PocketType.LIVING, "600000", "300000"),
                pocket(PocketType.HOUSING, "500000", "500000"),
                pocket(PocketType.EMERGENCY, "200000", "50000")
        ));

        List<PocketResponse> result = pocketService.findAll();

        assertThat(result).extracting(PocketResponse::getType)
                .containsExactly(PocketType.HOUSING, PocketType.LIVING,
                        PocketType.EMERGENCY, PocketType.ASSET);
    }

    @Test
    void 소진율은_소수점_한자리로_계산된다() {
        given(pocketRepository.findAll()).willReturn(List.of(
                pocket(PocketType.LIVING, "600000", "300000")
        ));

        PocketResponse result = pocketService.findAll().get(0);

        assertThat(result.getUsageRate()).isEqualByComparingTo("50.0");
        assertThat(result.getRemaining()).isEqualByComparingTo("300000");
    }

    @Test
    void 배분액이_0인_포켓도_0으로_나누지_않고_소진율_0을_준다() {
        // 아직 예산을 확정하지 않은 포켓이 이 경우다. 막지 않으면 ArithmeticException으로 목록 전체가 죽는다.
        given(pocketRepository.findAll()).willReturn(List.of(
                pocket(PocketType.ASSET, "0", "0")
        ));

        PocketResponse result = pocketService.findAll().get(0);

        assertThat(result.getUsageRate()).isEqualByComparingTo("0");
    }

    @Test
    void 예산을_초과하면_남은_금액이_음수로_나온다() {
        // 잘라서 0으로 만들지 않는다. 화면이 초과 사실을 경고로 보여줘야 하기 때문이다.
        given(pocketRepository.findAll()).willReturn(List.of(
                pocket(PocketType.EMERGENCY, "200000", "250000")
        ));

        PocketResponse result = pocketService.findAll().get(0);

        assertThat(result.getRemaining()).isEqualByComparingTo("-50000");
        assertThat(result.getUsageRate()).isEqualByComparingTo("125.0");
    }

    @Test
    void 없는_포켓을_조회하면_예외를_던진다() {
        given(pocketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("포켓을 찾을 수 없습니다.");
    }
}
