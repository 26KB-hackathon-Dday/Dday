package com.dday.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답 봉투. Spring Data의 {@link Page}를 그대로 직렬화하면 필드가 20개 넘게
 * 딸려와 계약이 지저분해지므로, 프론트가 실제로 쓰는 것만 추린다.
 *
 * <p>{@link ApiResponse}의 {@code data}에 담긴다.
 */
@Getter
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    /** 0-base 현재 페이지 번호 */
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    /** 마지막 페이지면 true */
    private boolean last;

    /** {@code Page<Entity>} 를 DTO로 매핑하며 감싼다. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
