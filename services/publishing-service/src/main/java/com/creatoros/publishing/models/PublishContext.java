package com.creatoros.publishing.models;

import com.creatoros.publishing.entities.ConnectedAccount;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublishContext {

    private PublishRequestEvent event;
    private ConnectedAccount connectedAccount;
}
