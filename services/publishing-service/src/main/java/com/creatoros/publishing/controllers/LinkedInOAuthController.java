package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.LinkedInOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/linkedin")
@RequiredArgsConstructor
public class LinkedInOAuthController {

    private final LinkedInOAuthService oAuthService;

    @GetMapping("/login")
    public String login() {
        return oAuthService.buildAuthorizationUrl();
    }

    @GetMapping("/callback")
    public String callback(@RequestParam String code) {
        oAuthService.handleCallback(code);
        return "LinkedIn connected successfully";
    }
}
