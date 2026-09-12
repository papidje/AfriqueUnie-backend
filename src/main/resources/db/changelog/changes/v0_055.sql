--liquibase formatted sql
--changeset friasoft:055-messaging
--comment: Messagerie 1-1 in-app (conversations, participants, messages).

CREATE TABLE IF NOT EXISTS schools.messaging_conversations (
    id BIGSERIAL PRIMARY KEY,
    last_message_at TIMESTAMP,
    last_message_preview VARCHAR(280),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messaging_conversations_last_message_at
    ON schools.messaging_conversations (last_message_at DESC NULLS LAST);

CREATE TABLE IF NOT EXISTS schools.messaging_participants (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES schools.messaging_conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES schools.users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP,
    CONSTRAINT uq_messaging_participants_conv_user UNIQUE (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_messaging_participants_user_id
    ON schools.messaging_participants (user_id);

CREATE INDEX IF NOT EXISTS idx_messaging_participants_conversation_id
    ON schools.messaging_participants (conversation_id);

CREATE TABLE IF NOT EXISTS schools.messaging_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES schools.messaging_conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES schools.users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messaging_messages_conversation_created
    ON schools.messaging_messages (conversation_id, created_at, id);
