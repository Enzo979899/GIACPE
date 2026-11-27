package dev.enzo.giacpe.identity.domain.repository;

import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    //SOLO PARA CREAR EL PRIMER ADMIN GLOBAL DEL MVP

    boolean existsByRole(UserRole role);
}
