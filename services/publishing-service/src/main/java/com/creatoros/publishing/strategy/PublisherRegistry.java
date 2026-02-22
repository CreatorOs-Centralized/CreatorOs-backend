package com.creatoros.publishing.strategy;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PublisherRegistry {

    private final Map<String, SocialPublisher> publishers;

    public PublisherRegistry(Map<String, SocialPublisher> publishers) {
        this.publishers = publishers;
    }

    public SocialPublisher getPublisher(String platform) {
        SocialPublisher publisher = publishers.get(platform.toUpperCase());
        if (publisher == null) {
            throw new RuntimeException("No publisher for platform " + platform);
        }
        return publisher;
    }
}
