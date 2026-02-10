package com.creatoros.publishing.strategy;

import com.creatoros.publishing.models.PublishContext;
import com.creatoros.publishing.models.PublishResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component("LINKEDIN")
@RequiredArgsConstructor
public class LinkedInPublisher implements SocialPublisher {

    private final RestTemplate restTemplate;

    @Override
    public PublishResult publish(PublishContext context) {
        try {
            String accessToken = context.getConnectedAccount().getAccessTokenEnc();
            String author = context.getConnectedAccount().getLinkedinAuthorUrn();

            Map<String, Object> payload = Map.of(
                    "author", author,
                    "lifecycleState", "PUBLISHED",
                    "specificContent", Map.of(
                            "com.linkedin.ugc.ShareContent", Map.of(
                                    "shareCommentary", Map.of(
                                            "text", "Hello from CreatorOS 🚀"
                                    ),
                                    "shareMediaCategory", "NONE"
                            )
                    ),
                    "visibility", Map.of(
                            "com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.linkedin.com/v2/ugcPosts",
                    request,
                    Map.class
            );

            String postUrn = response.getHeaders().getFirst("x-restli-id");

            return PublishResult.builder()
                    .success(true)
                    .platformPostId(postUrn)
                    .permalink("https://www.linkedin.com/feed/update/" + postUrn)
                    .build();

        } catch (Exception ex) {
            return PublishResult.builder()
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }
}
