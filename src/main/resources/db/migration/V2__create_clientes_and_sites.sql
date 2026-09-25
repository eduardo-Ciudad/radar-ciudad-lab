-- V2__create_clients_and_sites.sql
-- Liga os dois módulos: um lead fechado vira cliente; cliente tem sites; site tem propriedade GA4.

CREATE TABLE client (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    lead_id         BIGINT UNIQUE REFERENCES lead(id),  -- NULL para clientes que não vieram da prospecção
    contact_name    VARCHAR(255),
    contact_phone   VARCHAR(20),
    contact_email   VARCHAR(255),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE site (
    id                  BIGSERIAL PRIMARY KEY,
    client_id           BIGINT       REFERENCES client(id),  -- NULL = site próprio (ciudadlab.com.br, portfólio)
    name                VARCHAR(255) NOT NULL,
    url                 VARCHAR(500) NOT NULL UNIQUE,

    ga_property_id      VARCHAR(20)  UNIQUE,            -- só o número: "properties/123456789" -> 123456789
    ga_timezone         VARCHAR(50),                    -- fuso da propriedade: as datas do GA vêm nele
    ga_access_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (ga_access_status IN ('PENDING','OK','NO_ACCESS','ERROR')),
    ga_last_sync_at     TIMESTAMPTZ,
    ga_last_sync_error  TEXT,
    ga_backfilled_until DATE,                           -- até onde o histórico já foi puxado

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Quais eventos contam como "resultado" neste site (clique no WhatsApp, envio de formulário...)
CREATE TABLE site_key_event (
    site_id         BIGINT       NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    event_name      VARCHAR(100) NOT NULL,              -- nome no GA, ex.: whatsapp_click
    label           VARCHAR(100) NOT NULL,              -- como aparece no dashboard: "Cliques no WhatsApp"
    PRIMARY KEY (site_id, event_name)
);