package com.dday.domain.pocket.repository;

import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PocketRepository extends JpaRepository<Pocket, Long> {

    /**
     * 선언 순서(주거 → 생활 → 비상 → 자산형성)로 정렬해 돌려준다.
     *
     * <p>{@code type}이 {@code EnumType.STRING}이라 DB에서는 이름 알파벳순(ASSET, EMERGENCY…)이
     * 되어버린다. 화면에 필요한 건 그 순서가 아니라 <b>돈이 확보돼야 하는 우선순위</b>라
     * 정렬은 서비스에서 enum 순서로 다시 잡는다. 그래서 여기선 정렬을 걸지 않는다.
     */
    List<Pocket> findAll();

    Optional<Pocket> findByType(PocketType type);
}
