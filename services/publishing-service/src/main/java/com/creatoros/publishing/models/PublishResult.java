package com.creatoros.publishing.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublishResult {

    private boolean success;
    private String platformPostId;
    private String permalink;
    private String errorMessage;
}
