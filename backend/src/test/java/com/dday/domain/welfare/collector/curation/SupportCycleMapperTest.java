package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.SupportAmountType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportCycleMapperTest {

    @Test
    void 지원주기_매핑() {
        assertThat(SupportCycleMapper.toAmountType("월")).isEqualTo(SupportAmountType.MONTHLY);
        assertThat(SupportCycleMapper.toAmountType("1회성")).isEqualTo(SupportAmountType.FIXED);
        assertThat(SupportCycleMapper.toAmountType("반기")).isEqualTo(SupportAmountType.SEMIANNUAL);
    }

    @Test
    void 매핑_안되는_주기는_null() {
        assertThat(SupportCycleMapper.toAmountType("수시")).isNull();
        assertThat(SupportCycleMapper.toAmountType(null)).isNull();
        assertThat(SupportCycleMapper.toAmountType("연 1회")).isNull();
    }
}
