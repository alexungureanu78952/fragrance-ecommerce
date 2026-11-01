package com.fragrance.ecommerce.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
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

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.UserResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.toResponse(authService.register(request));
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
        var principal = (com.fragrance.ecommerce.security.AuthenticatedUser) authentication.getPrincipal();
        return new AuthDtos.UserResponse(
            principal.id(), principal.getUsername(), principal.getUsername(),
            principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
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
}
