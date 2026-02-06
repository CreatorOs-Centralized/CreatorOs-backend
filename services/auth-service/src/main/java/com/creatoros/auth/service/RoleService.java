package com.creatoros.auth.service;

import java.util.LinkedHashSet;
import java.util.Set;

import com.creatoros.auth.model.Role;
import com.creatoros.auth.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Set<Role> upsertRoles(Set<String> roleNames) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            return roles;
        }

        for (String roleName : roleNames) {
            if (roleName == null || roleName.isBlank()) {
                continue;
            }
            String normalized = roleName.trim();
            Role role = roleRepository.findByName(normalized)
                    .orElseGet(() -> roleRepository.save(new Role(normalized)));
            roles.add(role);
        }
        return roles;
    }
}
