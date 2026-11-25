package dev.enzo.giacpe.identity.infrastructure.persistence;

import dev.enzo.giacpe.identity.domain.model.User;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.getUserId(), user.getName(), user.getEmail(), user.getPasswordHash(),
                user.getUserRole(), user.getActivationStatus(), user.isEnabled(), user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getPasswordHash(), entity.getRole(),
                entity.getActivationStatus(), entity.isEnabled(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
