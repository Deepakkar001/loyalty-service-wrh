-- Reference tables for all dropdown values in onboarding UI
-- Using a consistent pattern matching ref_business_category / ref_country

CREATE TABLE IF NOT EXISTS ref_timezone (
    code        VARCHAR(50)  NOT NULL PRIMARY KEY,
    label       VARCHAR(120) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_timezone_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_business_model (
    code        VARCHAR(30)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_business_model_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_annual_revenue_range (
    code        VARCHAR(20)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_annual_revenue_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_payment_method_accepted (
    code        VARCHAR(30)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_payment_method_acc_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_settlement_frequency (
    code        VARCHAR(20)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_settlement_freq_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_currency (
    code        VARCHAR(10)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_currency_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_billing_payment_method (
    code        VARCHAR(30)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_billing_pm_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_contract_duration (
    code        VARCHAR(10)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ref_contract_dur_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══ Seed data ═══

INSERT INTO ref_timezone (code, label, sort_order) VALUES
  ('Asia/Kolkata',        'Asia/Kolkata (IST)',        10),
  ('America/New_York',    'America/New_York (EST)',    20),
  ('America/Los_Angeles', 'America/Los_Angeles (PST)', 30),
  ('Europe/London',       'Europe/London (GMT)',       40),
  ('Europe/Berlin',       'Europe/Berlin (CET)',       50),
  ('Asia/Singapore',      'Asia/Singapore (SGT)',      60),
  ('Asia/Dubai',          'Asia/Dubai (GST)',          70),
  ('Australia/Sydney',    'Australia/Sydney (AEST)',   80),
  ('UTC',                 'UTC',                       90)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_business_model (code, label, sort_order) VALUES
  ('D2C',         'Direct to Consumer (D2C)', 10),
  ('B2B',         'Business to Business (B2B)', 20),
  ('B2C',         'Business to Consumer (B2C)', 30),
  ('MARKETPLACE', 'Marketplace',               40),
  ('FRANCHISE',   'Franchise',                 50),
  ('HYBRID',      'Hybrid',                    60)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_annual_revenue_range (code, label, sort_order) VALUES
  ('LESS_THAN_1M',     'Less than $1M',  10),
  ('FROM_1M_TO_5M',    '$1M - $5M',      20),
  ('FROM_5M_TO_25M',   '$5M - $25M',     30),
  ('FROM_25M_TO_100M', '$25M - $100M',   40),
  ('OVER_100M',        '$100M+',         50)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_payment_method_accepted (code, label, sort_order) VALUES
  ('CREDIT_CARD',  'Credit Card',  10),
  ('DEBIT_CARD',   'Debit Card',   20),
  ('UPI',          'UPI',          30),
  ('NET_BANKING',  'Net Banking',  40),
  ('WALLET',       'Wallet',       50),
  ('CASH',         'Cash',         60),
  ('BNPL',         'BNPL',         70)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_settlement_frequency (code, label, sort_order) VALUES
  ('DAILY',    'Daily',   10),
  ('WEEKLY',   'Weekly',  20),
  ('MONTHLY',  'Monthly', 30),
  ('T_PLUS_1', 'T+1',     40)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_currency (code, label, sort_order) VALUES
  ('INR', 'INR - Indian Rupee',       10),
  ('USD', 'USD - US Dollar',          20),
  ('EUR', 'EUR - Euro',               30),
  ('GBP', 'GBP - British Pound',      40),
  ('SGD', 'SGD - Singapore Dollar',   50),
  ('AED', 'AED - UAE Dirham',         60),
  ('AUD', 'AUD - Australian Dollar',  70)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_billing_payment_method (code, label, sort_order) VALUES
  ('BANK_TRANSFER', 'Bank Transfer (NEFT/RTGS)', 10),
  ('CREDIT_CARD',   'Credit Card',               20),
  ('ACH',           'ACH',                       30),
  ('WIRE',          'Wire Transfer',             40),
  ('CHEQUE',        'Cheque',                    50)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ref_contract_duration (code, label, sort_order) VALUES
  ('6',  '6 months',  10),
  ('12', '12 months', 20),
  ('24', '24 months', 30),
  ('36', '36 months', 40)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);
