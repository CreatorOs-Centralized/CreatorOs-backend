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
        executeAndReturn(event);
    }

    public PublishExecutionOutcome executeAndReturn(PublishRequestEvent event) {
        PublishJob job = publishJobService.createJob(event);

        eventProducer.publishStarted(
            job.getUserId(),
            event.getEmail(),
            job.getId(),
            event.getPlatform()
        );

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
                return new PublishExecutionOutcome(job, result);
            }

            publishJobService.markFailure(job, result.getErrorMessage());
            emitPublishFailed(job, event, result.getErrorMessage());
            emitPublishRetryRequested(job, event, result.getErrorMessage());
            return new PublishExecutionOutcome(job, result);
        } catch (Exception ex) {
            publishJobService.markFailure(job, ex.getMessage());
            emitPublishFailed(job, event, ex.getMessage());
            emitPublishRetryRequested(job, event, ex.getMessage());
            return new PublishExecutionOutcome(
                    job,
                    PublishResult.builder()
                            .success(false)
                            .errorMessage(ex.getMessage())
                            .build()
            );
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
                context.getEvent().getEmail(),
                job.getId(),
                context.getEvent().getPlatform(),
                result.getPlatformPostId(),
                result.getPermalink()
        );
    }

    private void emitPublishFailed(PublishJob job, PublishRequestEvent event, String errorMessage) {
        eventProducer.publishFailed(
                job.getUserId(),
                event.getEmail(),
                job.getId(),
                event.getPlatform(),
                errorMessage == null ? "publish_failed" : errorMessage
        );
    }

    private void emitPublishRetryRequested(PublishJob job, PublishRequestEvent event, String reason) {
        if (job.getCurrentRetryCount() != null && job.getMaxRetries() != null
                && job.getCurrentRetryCount() >= job.getMaxRetries()) {
            return;
        }

        eventProducer.publishRetryRequested(
                job.getUserId(),
                event.getEmail(),
                job.getId(),
                event.getPlatform(),
                reason == null ? "retry_requested" : reason
        );
    }

    public record PublishExecutionOutcome(PublishJob job, PublishResult result) {
    }
}
