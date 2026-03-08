package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.MetaDataDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/oauth/meta/data-deletion")
@RequiredArgsConstructor
@Slf4j
public class MetaDataDeletionController {

    private final MetaDataDeletionService metaDataDeletionService;

    @PostMapping
    public ResponseEntity<?> handleDataDeletion(@RequestBody MultiValueMap<String, String> form) {
        try {
            String signedRequest = form.getFirst("signed_request");
            Map<String, Object> response = metaDataDeletionService.processDeletionCallback(signedRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.error("Meta data deletion callback failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage(), "success", false));
        } catch (Exception ex) {
            log.error("Unexpected Meta data deletion callback error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process data deletion callback", "success", false));
        }
    }

    @GetMapping("/status/{confirmationCode}")
    public ResponseEntity<?> getDataDeletionStatus(@PathVariable String confirmationCode) {
        try {
            Map<String, Object> status = metaDataDeletionService.getDeletionStatus(confirmationCode);
            return ResponseEntity.ok(status);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage(), "success", false));
        } catch (Exception ex) {
            log.error("Unexpected Meta data deletion status error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch data deletion status", "success", false));
        }
    }
}