-- Ishtirak schema: the eight tables of the proposal (section 2, data dictionary).
-- Amounts are Lebanese pounds (LBP), stored as whole numbers.

CREATE TABLE app_user (
    user_id       SERIAL       PRIMARY KEY,
    username      VARCHAR(40)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,           -- BCrypt hash only, never a plain password
    full_name     VARCHAR(80)  NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'COLLECTOR')),
    CONSTRAINT ck_app_user_bcrypt CHECK (password_hash LIKE '$2%')
);

CREATE TABLE zone (
    zone_id  SERIAL      PRIMARY KEY,
    name     VARCHAR(60) NOT NULL,
    is_rural BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_zone_name UNIQUE (name)
);

CREATE TABLE subscriber (
    subscriber_id SERIAL       PRIMARY KEY,
    zone_id       INT          NOT NULL REFERENCES zone (zone_id),
    full_name     VARCHAR(80)  NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    address       VARCHAR(150),
    status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscriber_phone UNIQUE (phone),
    CONSTRAINT ck_subscriber_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE subscription (
    subscription_id SERIAL         PRIMARY KEY,
    subscriber_id   INT            NOT NULL REFERENCES subscriber (subscriber_id),
    meter_serial    VARCHAR(30)    NOT NULL,
    amperage        SMALLINT       NOT NULL,
    initial_reading NUMERIC(10, 1) NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE,                            -- NULL while the subscription is active
    CONSTRAINT uq_subscription_meter_serial UNIQUE (meter_serial),
    CONSTRAINT ck_subscription_amperage CHECK (amperage BETWEEN 5 AND 60 AND amperage % 5 = 0),
    CONSTRAINT ck_subscription_initial_reading CHECK (initial_reading >= 0),
    CONSTRAINT ck_subscription_end_date CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE tariff_month (
    tariff_id        SERIAL         PRIMARY KEY,
    billing_month    DATE           NOT NULL,
    price_per_kwh    NUMERIC(12, 0) NOT NULL,
    fee_5a           NUMERIC(12, 0) NOT NULL,
    fee_10a          NUMERIC(12, 0) NOT NULL,
    fee_per_extra_5a NUMERIC(12, 0) NOT NULL,
    rural_uplift_pct NUMERIC(4, 1)  NOT NULL DEFAULT 10,
    CONSTRAINT uq_tariff_month_billing_month UNIQUE (billing_month),
    CONSTRAINT ck_tariff_month_first_day CHECK (EXTRACT(DAY FROM billing_month) = 1),
    CONSTRAINT ck_tariff_month_prices CHECK (price_per_kwh > 0 AND fee_5a > 0 AND fee_10a > 0 AND fee_per_extra_5a > 0),
    CONSTRAINT ck_tariff_month_uplift CHECK (rural_uplift_pct BETWEEN 0 AND 100)
);

CREATE TABLE meter_reading (
    reading_id      SERIAL         PRIMARY KEY,
    subscription_id INT            NOT NULL REFERENCES subscription (subscription_id),
    billing_month   DATE           NOT NULL,         -- logical join to tariff_month, deliberately not a FK
    reading_value   NUMERIC(10, 1) NOT NULL,
    read_at         TIMESTAMP      NOT NULL DEFAULT now(),
    read_by         INT            NOT NULL REFERENCES app_user (user_id),
    CONSTRAINT uq_meter_reading_subscription_month UNIQUE (subscription_id, billing_month),
    CONSTRAINT ck_meter_reading_value CHECK (reading_value >= 0),
    CONSTRAINT ck_meter_reading_first_day CHECK (EXTRACT(DAY FROM billing_month) = 1)
);

CREATE TABLE invoice (
    invoice_id      SERIAL         PRIMARY KEY,
    reading_id      INT            NOT NULL REFERENCES meter_reading (reading_id),
    consumption_kwh NUMERIC(10, 1) NOT NULL,         -- snapshot: an issued bill never changes
    amount_due      NUMERIC(14, 0) NOT NULL,         -- snapshot
    issued_at       TIMESTAMP      NOT NULL DEFAULT now(),
    voided          BOOLEAN        NOT NULL DEFAULT FALSE,
    void_reason     VARCHAR(200),
    CONSTRAINT ck_invoice_amount_due CHECK (amount_due >= 0),
    CONSTRAINT ck_invoice_void_reason CHECK (voided = FALSE OR void_reason IS NOT NULL)
);

-- At most one non-voided invoice per reading: makes the billing run idempotent at the database level.
CREATE UNIQUE INDEX uq_invoice_reading_not_voided ON invoice (reading_id) WHERE voided = FALSE;

CREATE TABLE payment (
    payment_id  SERIAL         PRIMARY KEY,
    invoice_id  INT            NOT NULL REFERENCES invoice (invoice_id),
    receipt_no  VARCHAR(20)    NOT NULL,
    amount      NUMERIC(14, 0) NOT NULL,
    method      VARCHAR(10)    NOT NULL,
    paid_at     TIMESTAMP      NOT NULL DEFAULT now(),
    received_by INT            NOT NULL REFERENCES app_user (user_id),
    CONSTRAINT uq_payment_receipt_no UNIQUE (receipt_no),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_method CHECK (method IN ('CASH', 'TRANSFER'))
);

-- Source of receipt numbers (a sequence, not a table)
CREATE SEQUENCE receipt_no_seq START 1;

-- Indexes on the foreign keys used by the reading sheet, billing run and reports
CREATE INDEX ix_subscriber_zone ON subscriber (zone_id);
CREATE INDEX ix_subscription_subscriber ON subscription (subscriber_id);
CREATE INDEX ix_meter_reading_month ON meter_reading (billing_month);
CREATE INDEX ix_payment_invoice ON payment (invoice_id);
