package com.dday.domain.mydata.service;

import com.dday.domain.mockmydata.service.MockMydataProvisioner;
import com.dday.domain.mydata.dto.response.MydataConnectResponse;
import com.dday.domain.mydata.repository.UserAccountRepository;
import com.dday.domain.pocket.service.PocketService;
import com.dday.domain.mydata.dto.response.MydataSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 마이데이터 연결. 온보딩의 "기관 선택 → 연동" 화면이 부른다.
 *
 * <p>네 가지를 한 번에 끝낸다. 화면에서는 버튼 하나라 왕복을 나누면 중간에 실패했을 때
 * "동의는 됐는데 데이터는 없는" 어중간한 상태가 남는다.
 *
 * <ol>
 *   <li><b>동의 기록</b> — {@code mydata_connected}와 만료시각을 남긴다.
 *       이게 없으면 뒤이은 동기화가 403으로 막힌다.</li>
 *   <li><b>목데이터 준비</b> — 시연에서 누가 가입하든 같은 계좌·카드가 보이도록
 *       데모 계정의 데이터를 복제한다 ({@link MockMydataProvisioner}).</li>
 *   <li><b>기본 포켓 보장</b> — 소비를 담을 포켓이 없으면 동기화의 자동분류가
 *       '자유 포켓'을 찾지 못해 404로 끝난다. 가입만으로는 포켓이 생기지 않는다.</li>
 *   <li><b>첫 동기화</b> — 목 서버의 거래를 우리 테이블로 옮긴다. 여기까지 해야
 *       연동 직후 화면에 소비 내역이 뜬다.</li>
 * </ol>
 *
 * <p>클래스에 {@code @Transactional}을 붙이지 않는다. 마지막 단계가 외부 공급자 호출을
 * 포함해 오래 걸리기 때문이다({@link MydataService} 주석 참고). 쓰기 원자성은 각 단계의
 * 짧은 트랜잭션이 맡는다.
 *
 * <p><b>⚠️ 2번은 해커톤 시연 전용이다.</b> 실제 마이데이터를 붙이면 그 단계만 빼면 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MydataConnectService {

    /**
     * 동의 유효기간(년). 실제 마이데이터는 법정 기간이 정해져 있고 만료되면 재동의를 받아야 한다.
     * 시연 중에 만료되지 않도록 넉넉히 잡는다.
     */
    private static final int CONSENT_VALID_YEARS = 1;

    private final MydataConsentWriter consentWriter;
    private final MockMydataProvisioner mockMydataProvisioner;
    private final MydataService mydataService;
    private final UserAccountRepository userAccountRepository;
    private final PocketService pocketService;

    /**
     * 이미 연결한 회원이 다시 눌러도 안전하다. 동의는 갱신되고, 목데이터는 이미 있으면
     * 건너뛰며, 동기화는 중복 거래를 스킵한다.
     */
    public MydataConnectResponse connect(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        String name = consentWriter.recordConsent(userId, now, now.plusYears(CONSENT_VALID_YEARS));
        boolean provisioned = mockMydataProvisioner.provision(userId, name);

        // 포켓이 없으면 동기화의 자동분류가 '자유 포켓'을 못 찾아 404로 끝난다.
        // initialize는 없는 것만 만들므로 이미 있으면 아무 일도 하지 않는다.
        pocketService.initialize(userId);

        MydataSyncResponse sync = mydataService.sync(userId, null, null);

        log.info("마이데이터 연결: userId={}, 목데이터 복제={}", userId, provisioned);

        // 동기화가 끝난 뒤 읽어야 이번에 새로 붙은 계좌까지 응답에 담긴다.
        return MydataConnectResponse.of(
                userAccountRepository.findAllByUserUserIdOrderByAccountIdAsc(userId),
                sync.getInsertedTransactionCount());
    }
}
