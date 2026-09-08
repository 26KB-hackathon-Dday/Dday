package com.dday.domain.pocket.service;

import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.CategoryRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void 활성_카테고리를_부모와_자식_트리로_구성한다() {
        Category parent = category(1L, null, "FOOD", "식비");
        Category child = category(2L, parent, "DELIVERY", "배달");
        given(categoryRepository.findAllActive(PocketType.FREE))
                .willReturn(List.of(parent, child));

        var result = categoryService.findAll(PocketType.FREE);

        assertThat(result.getCategories()).singleElement().satisfies(root -> {
            assertThat(root.getCategoryCode()).isEqualTo("FOOD");
            assertThat(root.getChildren()).singleElement()
                    .extracting(childResponse -> childResponse.getCategoryCode())
                    .isEqualTo("DELIVERY");
        });
    }

    @Test
    void 소비_카테고리가_없는_포켓_유형은_거부한다() {
        assertThatThrownBy(() -> categoryService.findAll(PocketType.EMERGENCY))
                .isInstanceOf(BusinessException.class)
                .hasMessage("소비 거래는 필수 또는 자유 포켓만 조회할 수 있습니다.");
    }

    private Category category(Long id, Category parent, String code, String name) {
        Category category = Category.builder()
                .parentCategory(parent)
                .categoryCode(code)
                .categoryName(name)
                .defaultPocketType(PocketType.FREE)
                .build();
        ReflectionTestUtils.setField(category, "categoryId", id);
        return category;
    }
}
