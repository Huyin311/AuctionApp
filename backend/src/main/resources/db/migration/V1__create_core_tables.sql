-- Combined schema creation + sales/escrow migration for Inner Auction
-- File: V2025_11_07__create_schema_and_sales.sql
-- Chạy trên môi trường test trước khi apply lên production.
-- Lưu ý: sử dụng uuid-ossp (uuid_generate_v4). Nếu PG không cho phép, thay bằng gen_random_uuid() và bật pgcrypto.
-- Script idempotent (dùng CREATE IF NOT EXISTS) để có thể chạy nhiều lần an toàn.

-- ============================
-- Optional cleanup (uncomment để drop nếu cần) - USE WITH CAUTION
-- ============================
-- DROP VIEW IF EXISTS v_system_revenue;
-- DROP TRIGGER IF EXISTS trg_users_set_updated_at ON users;
-- DROP TRIGGER IF EXISTS trg_auctions_set_updated_at ON auctions;
-- DROP TRIGGER IF EXISTS trg_payouts_set_updated_at ON payouts;
-- DROP FUNCTION IF EXISTS trigger_set_updated_at();
-- DROP TABLE IF EXISTS admin_audit CASCADE;
-- DROP TABLE IF EXISTS commission_logs CASCADE;
-- DROP TABLE IF EXISTS platform_balance CASCADE;
-- DROP TABLE IF EXISTS payouts CASCADE;
-- DROP TABLE IF EXISTS audit_logs CASCADE;
-- DROP TABLE IF EXISTS messages CASCADE;
-- DROP TABLE IF EXISTS auction_images CASCADE;
-- DROP TABLE IF EXISTS otps CASCADE;
-- DROP TABLE IF EXISTS transactions CASCADE;
-- DROP TABLE IF EXISTS holds CASCADE;
-- DROP TABLE IF EXISTS bids CASCADE;
-- DROP TABLE IF EXISTS auctions CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;
-- DROP TABLE IF EXISTS sales CASCADE;
-- DROP TABLE IF EXISTS shipments CASCADE;
-- DROP TABLE IF EXISTS delivery_confirmations CASCADE;
-- DROP TABLE IF EXISTS disputes CASCADE;
-- DROP TABLE IF EXISTS escrow_entries CASCADE;

-- ============================
-- Enable uuid extension
-- ============================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================
-- USERS
-- ============================
CREATE TABLE IF NOT EXISTS users (
                                     id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    email varchar(255) UNIQUE NOT NULL,
    password_hash varchar(512) NOT NULL,
    role varchar(20) NOT NULL DEFAULT 'BUYER',
    display_name varchar(255),
    phone varchar(50),
    deposit_paid boolean NOT NULL DEFAULT false,
    balance numeric(18,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz
    );

-- ============================
-- AUCTIONS
-- ============================
CREATE TABLE IF NOT EXISTS auctions (
                                        id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id uuid NOT NULL REFERENCES users(id),
    title varchar(300) NOT NULL,
    description text,
    starting_price numeric(18,2) NOT NULL CHECK (starting_price >= 0),
    current_price numeric(18,2),
    min_increment numeric(18,2) NOT NULL DEFAULT 1.00 CHECK (min_increment >= 0),
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    reserve_price numeric(18,2),
    status varchar(30) NOT NULL DEFAULT 'PUBLISHED',
    winner_id uuid REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    final_price numeric(18,2),
    commission_rate numeric(5,2) DEFAULT 5.00 CHECK (commission_rate >= 0 AND commission_rate <= 100),
    commission_amount numeric(18,2),
    settled boolean DEFAULT false,
    CHECK (start_at < end_at)
    );

CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_auctions_end_at ON auctions(end_at);

-- ============================
-- BIDS
-- ============================
CREATE TABLE IF NOT EXISTS bids (
                                    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id uuid NOT NULL REFERENCES auctions(id),
    user_id uuid NOT NULL REFERENCES users(id),
    amount numeric(18,2) NOT NULL CHECK (amount >= 0),
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_user_id ON bids(user_id);

-- ============================
-- HOLDS
-- ============================
CREATE TABLE IF NOT EXISTS holds (
                                     id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id),
    auction_id uuid NOT NULL REFERENCES auctions(id),
    amount numeric(18,2) NOT NULL CHECK (amount >= 0),
    status varchar(20) NOT NULL DEFAULT 'HELD', -- HELD | RELEASED | USED
    created_at timestamptz NOT NULL DEFAULT now(),
    released_at timestamptz
    );

CREATE INDEX IF NOT EXISTS idx_holds_auction_id ON holds(auction_id);
CREATE INDEX IF NOT EXISTS idx_holds_user_id ON holds(user_id);

-- ============================
-- TRANSACTIONS
-- ============================
CREATE TABLE IF NOT EXISTS transactions (
                                            id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id),
    type varchar(30) NOT NULL,
    amount numeric(18,2) NOT NULL CHECK (amount >= 0),
    reference_id uuid,
    related_entity varchar(50),
    status varchar(30) NOT NULL DEFAULT 'COMPLETED',
    created_at timestamptz NOT NULL DEFAULT now(),
    direction varchar(20),      -- IN | OUT | SYSTEM
    description text
    );

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);

-- ============================
-- OTPS
-- ============================
CREATE TABLE IF NOT EXISTS otps (
                                    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    email varchar(255) NOT NULL,
    code varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    verified boolean NOT NULL DEFAULT false,
    attempts int NOT NULL DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_otps_email ON otps(email);

-- ============================
-- AUCTION IMAGES
-- ============================
CREATE TABLE IF NOT EXISTS auction_images (
                                              id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id uuid NOT NULL REFERENCES auctions(id),
    url text NOT NULL,
    order_index int DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
    );

-- ============================
-- MESSAGES
-- ============================
CREATE TABLE IF NOT EXISTS messages (
                                        id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id uuid REFERENCES auctions(id),
    from_user uuid NOT NULL REFERENCES users(id),
    to_user uuid REFERENCES users(id),
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
    );

-- ============================
-- AUDIT LOGS
-- ============================
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_user_id uuid REFERENCES users(id),
    action varchar(100),
    entity varchar(50),
    entity_id uuid,
    payload jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
    );

-- ============================
-- PAYOUTS
-- ============================
CREATE TABLE IF NOT EXISTS payouts (
                                       id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id uuid NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    seller_id uuid NOT NULL REFERENCES users(id),
    total_amount numeric(18,2) NOT NULL CHECK (total_amount >= 0),
    commission_amount numeric(18,2) NOT NULL CHECK (commission_amount >= 0),
    net_amount numeric(18,2) NOT NULL CHECK (net_amount >= 0),
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    processed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_payouts_seller_id ON payouts(seller_id);

-- ============================
-- PLATFORM BALANCE
-- ============================
CREATE TABLE IF NOT EXISTS platform_balance (
                                                id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    balance numeric(18,2) NOT NULL DEFAULT 0.00,
    total_commission numeric(18,2) NOT NULL DEFAULT 0.00,
    last_updated timestamptz NOT NULL DEFAULT now()
    );

-- Ensure initial platform balance row
INSERT INTO platform_balance (balance, total_commission)
SELECT 0.00, 0.00
    WHERE NOT EXISTS (SELECT 1 FROM platform_balance);

-- ============================
-- COMMISSION LOGS
-- ============================
CREATE TABLE IF NOT EXISTS commission_logs (
                                               id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id uuid REFERENCES auctions(id) ON DELETE CASCADE,
    payout_id uuid REFERENCES payouts(id),
    seller_id uuid REFERENCES users(id),
    commission_amount numeric(18,2) NOT NULL CHECK (commission_amount >= 0),
    commission_rate numeric(5,2) NOT NULL CHECK (commission_rate >= 0 AND commission_rate <= 100),
    created_at timestamptz NOT NULL DEFAULT now(),
    note text
    );

CREATE INDEX IF NOT EXISTS idx_commission_logs_seller_id ON commission_logs(seller_id);

-- ============================
-- ADMIN AUDIT
-- ============================
CREATE TABLE IF NOT EXISTS admin_audit (
                                           id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_user_id uuid REFERENCES users(id),
    action varchar(100) NOT NULL,
    target_table varchar(50),
    target_id uuid,
    details jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
    );

-- ============================
-- SALES / SHIPMENTS / DELIVERY / DISPUTES / ESCROW (V2 additions)
-- ============================
CREATE TABLE IF NOT EXISTS sales (
                                     id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id uuid NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    buyer_id uuid NOT NULL REFERENCES users(id),
    seller_id uuid NOT NULL REFERENCES users(id),
    final_price numeric(18,2) NOT NULL,
    commission_rate numeric(5,2) NOT NULL,
    commission_amount numeric(18,2) NOT NULL,
    net_amount numeric(18,2) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'ESCROWED', -- ESCROWED | RELEASED | REFUNDED | DISPUTED
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    payout_id uuid
    );

CREATE INDEX IF NOT EXISTS idx_sales_auction_id ON sales(auction_id);
CREATE INDEX IF NOT EXISTS idx_sales_buyer_id ON sales(buyer_id);
CREATE INDEX IF NOT EXISTS idx_sales_seller_id ON sales(seller_id);

CREATE TABLE IF NOT EXISTS shipments (
                                         id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    carrier varchar(100),
    tracking_number varchar(200),
    label_url text,
    shipped_at timestamptz,
    estimated_delivery_at timestamptz,
    proof_of_shipment jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS delivery_confirmations (
                                                      id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    buyer_id uuid NOT NULL REFERENCES users(id),
    confirmed_at timestamptz NOT NULL DEFAULT now(),
    note text
    );

CREATE TABLE IF NOT EXISTS disputes (
                                        id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    opener_id uuid NOT NULL REFERENCES users(id),
    reason varchar(255),
    details text,
    status varchar(30) NOT NULL DEFAULT 'OPEN', -- OPEN | UNDER_REVIEW | RESOLVED | REJECTED
    resolution text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz
    );

CREATE TABLE IF NOT EXISTS escrow_entries (
                                              id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id uuid REFERENCES sales(id),
    user_id uuid NOT NULL REFERENCES users(id),
    amount numeric(18,2) NOT NULL,
    type varchar(30) NOT NULL, -- HOLD | ESCROW_IN | ESCROW_OUT | REFUND | RELEASE
    related_entity varchar(50),
    reference_id uuid,
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_escrow_sale_id ON escrow_entries(sale_id);
CREATE INDEX IF NOT EXISTS idx_disputes_sale_id ON disputes(sale_id);

-- ============================
-- INDEX ENSURE (redundant safe calls)
-- ============================
CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_holds_auction_id ON holds(auction_id);
CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);

-- ============================
-- TRIGGER FUNCTION: set updated_at
-- ============================
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
RETURN NEW;
END;
$$;

-- Attach triggers (if triggers already exist, you may see errors in some clients; drop triggers first if needed)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_users_set_updated_at'
  ) THEN
CREATE TRIGGER trg_users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
END IF;
END;
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_auctions_set_updated_at'
  ) THEN
CREATE TRIGGER trg_auctions_set_updated_at
    BEFORE UPDATE ON auctions
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
END IF;
END;
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_payouts_set_updated_at'
  ) THEN
CREATE TRIGGER trg_payouts_set_updated_at
    BEFORE UPDATE ON payouts
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
END IF;
END;
$$;

-- ============================
-- v_system_revenue view
-- ============================
CREATE OR REPLACE VIEW v_system_revenue AS
SELECT
    pb.balance AS current_balance,
    pb.total_commission AS total_commission,
    COALESCE((SELECT SUM(commission_amount) FROM commission_logs), 0) AS total_logged_commission,
    COALESCE((SELECT COUNT(*) FROM payouts), 0) AS total_payouts,
    (SELECT MAX(created_at) FROM commission_logs) AS last_commission_time
FROM platform_balance pb;

-- ============================
-- SCRIPT COMPLETE
-- ============================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url text;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS bio text;

-- Add updated_at column to holds and populate from created_at for existing rows
-- Test on staging first.

ALTER TABLE holds
    ADD COLUMN IF NOT EXISTS updated_at timestamptz;

-- Populate updated_at with created_at for existing rows to keep consistency
UPDATE holds
SET updated_at = created_at
WHERE updated_at IS NULL;

-- Optional: ensure index on auction_id,status and user_id,status exist (improves queries)
CREATE INDEX IF NOT EXISTS idx_hold_auction_status ON holds (auction_id, status);
CREATE INDEX IF NOT EXISTS idx_hold_user_status ON holds (user_id, status);

-- (Optional, Postgres only) Consider unique partial index to prevent duplicate HELD rows per user+auction
-- Make sure duplicates are merged before enabling this constraint.
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_hold_user_auction_held ON holds (user_id, auction_id) WHERE status = 'HELD';