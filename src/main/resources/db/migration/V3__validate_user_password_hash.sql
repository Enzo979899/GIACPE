ALTER TABLE users
DROP CONSTRAINT chk_users_activation_credentials;

ALTER TABLE users
    ADD CONSTRAINT chk_users_activation_credentials
        CHECK (
            activation_status <> 'ACTIVATED'
                OR (
                password_hash IS NOT NULL
                    AND LENGTH(TRIM(password_hash)) > 0
                )
            );
