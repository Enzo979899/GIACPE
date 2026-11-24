ALTER TABLE users
    ADD CONSTRAINT chk_users_pending_disabled
        CHECK (
            activation_status <> 'PENDING'
                OR enabled = FALSE
            );
