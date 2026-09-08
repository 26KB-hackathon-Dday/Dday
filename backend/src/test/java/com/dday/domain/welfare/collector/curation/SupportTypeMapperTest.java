package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.SupportType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportTypeMapperTest {

    @Test
    void 급여형태_매핑() {
        assertThat(SupportTypeMapper.from("현금지급")).isEqualTo(SupportType.CASH);
        assertThat(SupportTypeMapper.from("현금대여(융자)")).isEqualTo(SupportType.LOAN);
        assertThat(SupportTypeMapper.from("이용권")).isEqualTo(SupportType.VOUCHER);
        assertThat(SupportTypeMapper.from("프로그램/서비스(서비스)")).isEqualTo(SupportType.SERVICE);
    }

    @Test
    void 콤마_조합이면_현금지급이_있으면_CASH() {
        assertThat(SupportTypeMapper.from("현금지급,프로그램/서비스(서비스)")).isEqualTo(SupportType.CASH);
    }

    @Test
    void 현금지급이_없는_조합은_먼저_걸리는_것() {
        assertThat(SupportTypeMapper.from("프로그램/서비스(서비스),기타")).isEqualTo(SupportType.SERVICE);
    }

    @Test
    void 기타_null_빈값은_null() {
        assertThat(SupportTypeMapper.from("기타")).isNull();
        assertThat(SupportTypeMapper.from(null)).isNull();
        assertThat(SupportTypeMapper.from("")).isNull();
    }
}
