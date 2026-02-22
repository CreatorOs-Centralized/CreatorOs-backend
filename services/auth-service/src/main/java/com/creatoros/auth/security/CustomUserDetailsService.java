package com.creatoros.auth.security;

import java.util.LinkedHashSet;
import java.util.Set;

import com.creatoros.auth.model.Role;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findWithRolesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            if (role == null || role.getName() == null || role.getName().isBlank()) {
                continue;
            }
            String normalized = role.getName().startsWith("ROLE_") ? role.getName() : "ROLE_" + role.getName();
            authorities.add(new SimpleGrantedAuthority(normalized));
        }

        return new UserPrincipal(
            user.getId().toString(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                user.isEmailVerified(),
                authorities
        );
    }
}
