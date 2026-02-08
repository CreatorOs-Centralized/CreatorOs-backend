package com.creatoros.content.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentVersionRequest {

    @NotBlank(message = "Content body cannot be empty")
    private String body;
}
