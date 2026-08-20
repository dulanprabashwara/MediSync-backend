package com.medisync.user.controller;

import com.medisync.user.dto.UserResponse;
import com.medisync.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RoleProfileController {

    private final UserService userService;

    public RoleProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/patient/profile", "/pharmacist/profile", "/admin/profile"})
    public UserResponse roleProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(jwt);
    }
}
