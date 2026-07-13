package com.procurementsaas.identity.repo;

import com.procurementsaas.identity.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByKeycloakId(String keycloakId);
    boolean existsByUsername(String username);
}
