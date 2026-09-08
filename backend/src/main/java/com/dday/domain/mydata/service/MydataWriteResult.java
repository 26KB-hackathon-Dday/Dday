package com.dday.domain.mydata.service;

import lombok.Getter;

/** 거래 저장 단계에서 신규·중복·취소 연결 건수를 누적한다. */
@Getter
public class MydataWriteResult {
    private int inserted;
    private int skipped;
    private int linkedCancellation;

    public void addInserted() {
        inserted++;
    }

    public void addSkipped() {
        skipped++;
    }

    public void addLinkedCancellation() {
        linkedCancellation++;
    }
}
