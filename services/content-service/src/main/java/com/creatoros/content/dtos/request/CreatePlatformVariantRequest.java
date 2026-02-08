package com.creatoros.content.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlatformVariantRequest {

    @NotBlank
    private String platform; // INSTAGRAM, LINKEDIN, YOUTUBE

    @NotBlank
    private String variantType; // CAPTION, TITLE, DESCRIPTION, HASHTAGS

    @NotBlank
    private String value; // The actual content
}
