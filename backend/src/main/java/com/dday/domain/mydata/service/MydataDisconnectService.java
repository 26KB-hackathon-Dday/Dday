package com.dday.domain.mydata.service;

import com.dday.domain.mydata.dto.MydataErrorCode;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.entity.UserCard;
import com.dday.domain.mydata.repository.UserAccountRepository;
import com.dday.domain.mydata.repository.UserCardRepository;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 마이페이지 "금융정보 연결 관리"의 연결 해제. 기관 하나의 계좌·카드 연결을 끊는다.
 *
 * <p>행을 지우지 않고 {@link UserAccount#deactivate()}·{@link UserCard#deactivate()}로
 * 표시만 내린다 — 지우면 그 계좌·카드에 달린 과거 거래가 통째로 사라져 지난달 결산이 바뀐다.
 * 회원의 마지막 기관 연결까지 끊기면 {@code mydataConnected}도 함께 내린다.
 */
@Service
@RequiredArgsConstructor
public class MydataDisconnectService {

    private final UserAccountRepository userAccountRepository;
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;

    @Transactional
    public void disconnect(Long userId, String orgCode) {
        List<UserAccount> accounts = userAccountRepository.findAllByUserUserIdAndOrgCode(userId, orgCode);
        List<UserCard> cards = userCardRepository.findAllByUserUserIdAndOrgCode(userId, orgCode);

        if (accounts.isEmpty() && cards.isEmpty()) {
            throw new BusinessException(MydataErrorCode.INSTITUTION_NOT_CONNECTED);
        }

        accounts.forEach(UserAccount::deactivate);
        cards.forEach(UserCard::deactivate);

        boolean anyActiveLeft = userAccountRepository.existsByUserUserIdAndActiveTrue(userId)
                || userCardRepository.existsByUserUserIdAndActiveTrue(userId);
        if (!anyActiveLeft) {
            User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
            user.disconnectMydata();
        }
    }
}
