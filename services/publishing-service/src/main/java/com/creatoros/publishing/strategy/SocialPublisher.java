package com.creatoros.publishing.strategy;

import com.creatoros.publishing.models.PublishContext;
import com.creatoros.publishing.models.PublishResult;

public interface SocialPublisher {
    PublishResult publish(PublishContext context);
}
