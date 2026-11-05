-- V5__add_token_version_to_app_user.sql
-- Vai no claim "ver" do JWT. Somar 1 revoga todos os tokens já emitidos para o usuário (logout, desativação).
ALTER TABLE app_user ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;