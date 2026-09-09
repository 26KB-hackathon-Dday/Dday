package com.dday.domain.mydata.service;

import com.dday.domain.mydata.dto.response.UserCardListResponse;
import com.dday.domain.mydata.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지의 금융정보 관리 화면. 연동된 카드 목록을 보여준다.
 *
 * <p>{@link UserAccountService}의 카드 버전이다 — 카드를 추가·해지하는 건 여기서 하지
 * 않는다. 그건 MyData 쪽(연동·동기화·연결 해제)의 몫이다.
 */
@Service
@RequiredArgsConstructor
public class UserCardService {

    private final UserCardRepository userCardRepository;

    @Transactional(readOnly = true)
    public UserCardListResponse getCards(Long userId) {
        return UserCardListResponse.of(
                userCardRepository.findAllByUserUserIdOrderByCardIdAsc(userId));
    }
}
