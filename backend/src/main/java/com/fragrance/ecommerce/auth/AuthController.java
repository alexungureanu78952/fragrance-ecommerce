package com.fragrance.ecommerce.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import com.fragrance.ecommerce.security.AuthenticatedUser;
import com.fragrance.ecommerce.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          AuditLogService auditLogService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.UserResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request,
                                          HttpServletRequest servletRequest) {
        var user = authService.register(request);
        auditLogService.record(user.getId(), "REGISTER", "USER", user.getId().toString(),
            "SUCCESS", servletRequest.getHeader("X-Request-ID"));
        return authService.toResponse(user);
    }

    @PostMapping("/login")
    public AuthDtos.UserResponse login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                       HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        var authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        var principal = (AuthenticatedUser) authentication.getPrincipal();
        auditLogService.record(principal.id(), "LOGIN", "USER", principal.id().toString(),
            "SUCCESS", servletRequest.getHeader("X-Request-ID"));
        return new AuthDtos.UserResponse(
            principal.id(), principal.getUsername(), principal.displayName(),
            principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        var principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal != null && principal.getPrincipal() instanceof AuthenticatedUser user) {
            auditLogService.record(user.id(), "LOGOUT", "USER", user.id().toString(),
                "SUCCESS", request.getHeader("X-Request-ID"));
        }
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return new AuthDtos.UserResponse(
            principal.id(), principal.getUsername(), principal.displayName(),
            principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }
}
