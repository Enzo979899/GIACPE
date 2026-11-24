
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       name VARCHAR(150) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255),

                       role VARCHAR(30) NOT NULL,
                       activation_status VARCHAR(30) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT FALSE,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT chk_users_role
                           CHECK (role IN ('GLOBAL_ADMIN', 'MANAGER', 'FIELD_OFFICER')),

                       CONSTRAINT chk_users_activation_status
                           CHECK (activation_status IN ('PENDING', 'ACTIVATED')),

                       CONSTRAINT chk_users_activation_credentials
                           CHECK (
                               activation_status <> 'ACTIVATED'
                                   OR password_hash IS NOT NULL
                               )
);

CREATE UNIQUE INDEX uq_users_email_normalized
    ON users (LOWER(email));
