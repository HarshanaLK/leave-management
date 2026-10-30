-- V1 defined token_hash CHAR(64), while the PasswordResetToken entity maps
-- it as VARCHAR(64). Hibernate ddl-auto=validate requires matching SQL types.
-- Keep V1 unchanged so Flyway validation succeeds for existing installations.
ALTER TABLE password_reset_tokens
    MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;
