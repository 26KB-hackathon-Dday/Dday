package com.dday.domain.pocket.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포켓 표시 정보를 부분 수정하는 요청이다.
 *
 * <p>PATCH 요청이므로 {@code null}은 해당 필드를 보내지 않았다는 뜻이다. 이름의 공백 여부는
 * trim 이후 판단해야 하므로 서비스에서 검증하고, 길이는 요청 경계에서 먼저 차단한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PocketUpdateRequest {

    @Size(max = 50, message = "포켓 이름은 50자 이하여야 합니다.")
    private String pocketName;

    @Size(max = 500, message = "포켓 설명은 500자 이하여야 합니다.")
    private String description;
}
