-- V3__create_analytics.sql
-- Dados do GA4 em cache local. O dashboard lê SÓ daqui; a API do Google é chamada apenas pelo job de sync.
--
-- Regra de ouro: guardar só métricas SOMÁVEIS (contagens e durações totais).
-- Taxas e médias (engagementRate, averageSessionDuration) são calculadas na consulta,
-- porque média de médias diárias dá número errado ao agregar por semana/mês.

-- 1) Totais do site por dia
CREATE TABLE ga_daily_metrics (
    site_id                     BIGINT  NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    date                        DATE    NOT NULL,
    sessions                    INT     NOT NULL DEFAULT 0,
    engaged_sessions            INT     NOT NULL DEFAULT 0,  -- engagementRate = engaged/sessions
    new_users                   INT     NOT NULL DEFAULT 0,  -- somável (cada usuário é novo uma vez só)
    active_users                INT     NOT NULL DEFAULT 0,  -- NÃO somável entre dias (ver ga_period_users)
    page_views                  INT     NOT NULL DEFAULT 0,  -- screenPageViews
    event_count                 INT     NOT NULL DEFAULT 0,
    key_events                  INT     NOT NULL DEFAULT 0,  -- conversões (antigo "conversions")
    engagement_duration_seconds BIGINT  NOT NULL DEFAULT 0,  -- userEngagementDuration; tempo médio = isto/active_users
    synced_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (site_id, date)
);

-- 2) Quebras por dimensão, numa tabela só (evita 6 tabelas quase iguais)
CREATE TABLE ga_daily_breakdown (
    site_id             BIGINT       NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    date                DATE         NOT NULL,
    dimension           VARCHAR(20)  NOT NULL
                        CHECK (dimension IN ('CHANNEL','SOURCE','LANDING_PAGE','PAGE','DEVICE','CITY','EVENT')),
    value               VARCHAR(500) NOT NULL,           -- ex.: 'Organic Search', '/contato', 'mobile', 'whatsapp_click'
    sessions            INT          NOT NULL DEFAULT 0,
    engaged_sessions    INT          NOT NULL DEFAULT 0,
    page_views          INT          NOT NULL DEFAULT 0,
    event_count         INT          NOT NULL DEFAULT 0,
    key_events          INT          NOT NULL DEFAULT 0,
    synced_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (site_id, date, dimension, value)
);
CREATE INDEX idx_breakdown_lookup ON ga_daily_breakdown (site_id, dimension, date);

-- 3) Usuários únicos por período (não dá para somar usuários diários: a mesma pessoa conta várias vezes)
CREATE TABLE ga_period_users (
    site_id         BIGINT      NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    period_type     VARCHAR(10) NOT NULL CHECK (period_type IN ('WEEK','MONTH')),
    period_start    DATE        NOT NULL,
    period_end      DATE        NOT NULL,
    total_users     INT         NOT NULL,
    active_users    INT         NOT NULL,
    new_users       INT         NOT NULL,
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (site_id, period_type, period_start)
);

-- 4) Log de cada execução do sync (debug + monitorar consumo de cota)
CREATE TABLE ga_sync_run (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT       NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    date_from       DATE         NOT NULL,
    date_to         DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('RUNNING','SUCCESS','PARTIAL','FAILED')),
    requests_made   INT          NOT NULL DEFAULT 0,
    tokens_consumed INT,                                -- de returnPropertyQuota=true
    tokens_remaining_day INT,
    error_message   TEXT
);

-- 5) Insights: resumos da IA e alertas automáticos (queda de tráfego, tag parada...)
CREATE TABLE site_insight (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT       NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('WEEKLY_SUMMARY','MONTHLY_SUMMARY','ALERT')),
    severity        VARCHAR(10)  CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    period_start    DATE,
    period_end      DATE,
    title           VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    input_snapshot  JSONB,                              -- números enviados à IA: permite auditar o resumo
    model           VARCHAR(50),                        -- ex.: gemini-2.5-flash
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    read_at         TIMESTAMPTZ
);
CREATE INDEX idx_insight_site ON site_insight (site_id, created_at DESC);