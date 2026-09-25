
CREATE TABLE import_batch (
    id              BIGSERIAL PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_hash       CHAR(64)     NOT NULL UNIQUE,      -- SHA-256: impede importar o mesmo arquivo 2x
    imported_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    total_rows      INT          NOT NULL DEFAULT 0,
    created_count   INT          NOT NULL DEFAULT 0,
    updated_count   INT          NOT NULL DEFAULT 0,
    skipped_count   INT          NOT NULL DEFAULT 0,
    raw_content     TEXT                               -- arquivo original, para reprocessar se o parser mudar
);

CREATE TABLE lead (
    id                  BIGSERIAL PRIMARY KEY,
    -- chave de reimportação: nome normalizado + telefone E.164 (ou endereço, se não houver telefone)
    dedup_key           VARCHAR(255) NOT NULL UNIQUE,

    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(100),
    phone_e164          VARCHAR(20),                   -- +5517997726959
    is_mobile           BOOLEAN,                       -- 9º dígito => provável WhatsApp

    -- endereço decomposto: "R. X, 367 - Sala 1 - Bairro, Cidade - UF, 00000-000"
    address_raw         TEXT         NOT NULL,
    street              VARCHAR(255),
    number              VARCHAR(20),
    complement          VARCHAR(100),
    neighborhood        VARCHAR(120),
    city                VARCHAR(120),
    state               CHAR(2),
    zip_code            CHAR(9),
    address_normalized  TEXT,                          -- p/ detectar leads no mesmo endereço

    -- NULL = desconhecido (o exportador deixa vazio sempre; não significa "não tem")
    website_url         VARCHAR(500),
    instagram_handle    VARCHAR(100),

    -- último valor conhecido (histórico em lead_snapshot)
    rating              NUMERIC(2,1) CHECK (rating BETWEEN 0 AND 5),
    reviews_count       INT,                           -- não vem no CSV atual

    status              VARCHAR(20)  NOT NULL DEFAULT 'NOVO'
                        CHECK (status IN ('NOVO','CONTATADO','REUNIAO','PROPOSTA','FECHADO','PERDIDO','DESCARTADO')),
    score               INT,
    score_override      INT,                           -- ajuste manual, nunca sobrescrito pela importação
    notes               TEXT,                          -- idem
    ai_summary          TEXT,

    first_batch_id      BIGINT REFERENCES import_batch(id),
    last_batch_id       BIGINT REFERENCES import_batch(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Telefone e endereço NÃO são únicos: consultórios dividem recepção/prédio
CREATE INDEX idx_lead_phone   ON lead (phone_e164);
CREATE INDEX idx_lead_address ON lead (address_normalized);
CREATE INDEX idx_lead_status  ON lead (status);

-- Evolução de nota/avaliações a cada importação (gráfico "lead aquecendo")
CREATE TABLE lead_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    lead_id         BIGINT       NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    batch_id        BIGINT       NOT NULL REFERENCES import_batch(id),
    rating          NUMERIC(2,1),
    reviews_count   INT,
    captured_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (lead_id, batch_id)
);

-- Pipeline: permite medir tempo em cada etapa e taxa de conversão
CREATE TABLE lead_status_history (
    id              BIGSERIAL PRIMARY KEY,
    lead_id         BIGINT       NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    from_status     VARCHAR(20),
    to_status       VARCHAR(20)  NOT NULL,
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    note            TEXT
);

-- Leads que dividem telefone ou endereço (ex.: 3 profissionais no mesmo prédio)
CREATE VIEW lead_shared_location AS
SELECT a.id AS lead_id, b.id AS related_lead_id,
       CASE WHEN a.phone_e164 = b.phone_e164 THEN 'PHONE' ELSE 'ADDRESS' END AS match_type
FROM lead a
JOIN lead b ON a.id < b.id
 AND (a.phone_e164 = b.phone_e164
      OR (a.street = b.street AND a.number = b.number AND a.zip_code = b.zip_code));