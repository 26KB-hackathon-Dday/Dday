package com.dday.domain.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegionRequest {

    @NotBlank(message = "지역 코드를 입력해주세요.")
    @Size(max = 10, message = "지역 코드는 10자 이하로 입력해주세요.")
    private String regionCode;

    @NotBlank(message = "시·도를 입력해주세요.")
    @Size(max = 30, message = "시·도는 30자 이하로 입력해주세요.")
    private String regionName;

    @NotBlank(message = "시·군·구를 입력해주세요.")
    @Size(max = 30, message = "시·군·구는 30자 이하로 입력해주세요.")
    private String districtName;
}
