-- V4__create_app_user.sql
-- Usuários do Radar. Senha guardada só como hash BCrypt ({bcrypt}$2a$...), nunca em texto.

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'VIEWER'
                    CHECK (role IN ('ADMIN', 'VIEWER')),   -- ADMIN: tudo | VIEWER: só leitura (ex.: cliente vendo o próprio painel)
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);

-- E-mail único sem diferenciar maiúsculas: Eduardo@x.com e eduardo@x.com são o mesmo usuário
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));