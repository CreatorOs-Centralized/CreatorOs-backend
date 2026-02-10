package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.PublishJob;
import com.creatoros.publishing.entities.PublishedPost;
import com.creatoros.publishing.kafka.producers.PublishEventProducer;
import com.creatoros.publishing.models.PublishContext;
import com.creatoros.publishing.models.PublishRequestEvent;
import com.creatoros.publishing.models.PublishResult;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import com.creatoros.publishing.repositories.PublishedPostRepository;
import com.creatoros.publishing.strategy.PublisherRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PublishExecutionService {

    private final ConnectedAccountRepository accountRepository;
    private final PublisherRegistry publisherRegistry;
    private final PublishJobService publishJobService;
    private final PublishedPostRepository publishedPostRepository;
    private final PublishEventProducer eventProducer;

    public void execute(PublishRequestEvent event) {
        PublishJob job = publishJobService.createJob(event);

        try {
            PublishContext context = PublishContext.builder()
                    .event(event)
                    .connectedAccount(
                        accountRepository.findById(event.getConnectedAccountId())
                                    .orElseThrow(() -> 
                                        new RuntimeException("Connected account not found: " + event.getConnectedAccountId())
                                    )
                    )
                    .build();

            PublishResult result = publisherRegistry
                    .getPublisher(event.getPlatform())
                    .publish(context);

            if (result.isSuccess()) {
                publishJobService.markSuccess(job, result);
                persistPublishedPost(job, context, result);
                emitPublishSucceeded(job, context, result);
            } else {
                publishJobService.markFailure(job, result.getErrorMessage());
            }
        } catch (Exception ex) {
            publishJobService.markFailure(job, ex.getMessage());
        }
    }

    private void persistPublishedPost(PublishJob job, PublishContext context, PublishResult result) {
        PublishedPost post = PublishedPost.builder()
                .publishJobId(job.getId())
                .connectedAccountId(context.getConnectedAccount().getId())
                .platform(context.getEvent().getPlatform())
                .platformPostId(result.getPlatformPostId())
                .permalinkUrl(result.getPermalink())
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        publishedPostRepository.save(post);
    }

    private void emitPublishSucceeded(PublishJob job, PublishContext context, PublishResult result) {
        eventProducer.publishSuccess(
                job.getUserId(),
                job.getId(),
                context.getEvent().getPlatform(),
                result.getPlatformPostId(),
                result.getPermalink()
        );
    }
}
