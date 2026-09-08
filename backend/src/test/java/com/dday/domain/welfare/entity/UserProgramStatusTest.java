package com.dday.domain.welfare.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProgramStatusTest {

    private final UUID userId = UUID.randomUUID();

    private UserProgramStatus newStatus() {
        return new UserProgramStatus(userId, "WLF00001175");
    }

    @Test
    void 생성시_UNKNOWN_비관심_기본값() {
        var s = newStatus();

        assertThat(s.getReceivingStatus()).isEqualTo(ReceivingStatus.UNKNOWN);
        assertThat(s.getDetectionSource()).isNull();
        assertThat(s.isFavorite()).isFalse();
        assertThat(s.getFavoritedAt()).isNull();
    }

    @Test
    void 사용자가_수급중으로_확정하면_출처가_USER_INPUT() {
        var s = newStatus();

        s.updateReceiving(ReceivingStatus.RECEIVING, DetectionSource.USER_INPUT, null);

        assertThat(s.getReceivingStatus()).isEqualTo(ReceivingStatus.RECEIVING);
        assertThat(s.getDetectionSource()).isEqualTo(DetectionSource.USER_INPUT);
        assertThat(s.getDetectionEvidence()).isNull();
    }

    @Test
    void 자동탐지_확정이면_근거를_보관한다() {
        var s = newStatus();

        s.updateReceiving(ReceivingStatus.RECEIVING, DetectionSource.AUTO_DETECTED,
                "{\"matchedDeposits\":3,\"avgAmount\":500000}");

        assertThat(s.getDetectionSource()).isEqualTo(DetectionSource.AUTO_DETECTED);
        assertThat(s.getDetectionEvidence()).contains("matchedDeposits");
    }

    @Test
    void UNKNOWN으로_되돌리면_출처와_근거를_지운다() {
        var s = newStatus();
        s.updateReceiving(ReceivingStatus.RECEIVING, DetectionSource.AUTO_DETECTED, "{\"x\":1}");

        s.updateReceiving(ReceivingStatus.UNKNOWN, null, null);

        assertThat(s.getReceivingStatus()).isEqualTo(ReceivingStatus.UNKNOWN);
        assertThat(s.getDetectionSource()).isNull();
        assertThat(s.getDetectionEvidence()).isNull();
    }

    @Test
    void 관심등록하면_favoritedAt이_찍히고_해제하면_지워진다() {
        var s = newStatus();

        s.setFavorite(true);
        assertThat(s.isFavorite()).isTrue();
        assertThat(s.getFavoritedAt()).isNotNull();

        s.setFavorite(false);
        assertThat(s.isFavorite()).isFalse();
        assertThat(s.getFavoritedAt()).isNull();
    }

    @Test
    void 같은_값으로_setFavorite하면_아무것도_안_바뀐다() {
        var s = newStatus();
        var before = s.getStatusUpdatedAt();

        s.setFavorite(false);

        assertThat(s.getStatusUpdatedAt()).isEqualTo(before);
    }
}
