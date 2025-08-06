// Create new file: api/src/main/java/com/botofholding/api/Controller/AuthController.java
package com.botofholding.api.Controller;

import com.botofholding.api.Security.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/bot-token")
    public Map<String, String> getBotToken() {
        String token = jwtService.generateBotToken();
        return Map.of("token", token);
    }
}