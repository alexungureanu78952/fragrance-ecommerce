package com.fragrance.ecommerce.auth;

import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Role;
import com.fragrance.ecommerce.domain.User;
import com.fragrance.ecommerce.exception.ConflictException;
import com.fragrance.ecommerce.repository.RoleRepository;
import com.fragrance.ecommerce.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(AuthDtos.RegisterRequest request) {
        var email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already exists for this email");
        }
        Role customerRole = roleRepository.findByCode("CUSTOMER")
            .orElseThrow(() -> new IllegalStateException("Customer role is not configured"));
        var customer = new Customer(
            email,
            passwordEncoder.encode(request.password()),
            request.displayName().trim(),
            customerRole
        );
        return userRepository.save(customer);
    }

    public AuthDtos.UserResponse toResponse(User user) {
        return new AuthDtos.UserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getRoles().stream().map(Role::getCode).collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }
}
