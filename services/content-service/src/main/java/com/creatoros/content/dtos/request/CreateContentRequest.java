package com.creatoros.content.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContentRequest {

    @NotBlank
    private String title;

    private String contentType;
}
