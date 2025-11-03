-- USERS
CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  email varchar(255) UNIQUE NOT NULL,
  password_hash varchar(512) NOT NULL,
  role varchar(20) NOT NULL DEFAULT 'BUYER',
  display_name varchar(255),
  phone varchar(50),
  deposit_paid boolean NOT NULL DEFAULT false,
  balance numeric(18,2) NOT NULL DEFAULT 0.00,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz
);

-- AUCTIONS
CREATE TABLE auctions (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  seller_id uuid NOT NULL REFERENCES users(id),
  title varchar(300) NOT NULL,
  description text,
  starting_price numeric(18,2) NOT NULL,
  current_price numeric(18,2),
  min_increment numeric(18,2) NOT NULL DEFAULT 1.00,
  start_at timestamptz NOT NULL,
  end_at timestamptz NOT NULL,
  reserve_price numeric(18,2),
  status varchar(30) NOT NULL DEFAULT 'PUBLISHED',
  winner_id uuid REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz
);

-- BIDS
CREATE TABLE bids (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  auction_id uuid NOT NULL REFERENCES auctions(id),
  user_id uuid NOT NULL REFERENCES users(id),
  amount numeric(18,2) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- HOLDS
CREATE TABLE holds (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL REFERENCES users(id),
  auction_id uuid NOT NULL REFERENCES auctions(id),
  amount numeric(18,2) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'HELD',
  created_at timestamptz NOT NULL DEFAULT now(),
  released_at timestamptz
);

-- TRANSACTIONS
CREATE TABLE transactions (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL REFERENCES users(id),
  type varchar(30) NOT NULL,
  amount numeric(18,2) NOT NULL,
  reference_id uuid,
  related_entity varchar(50),
  status varchar(30) NOT NULL DEFAULT 'COMPLETED',
  created_at timestamptz NOT NULL DEFAULT now()
);

-- AUCTION IMAGES
CREATE TABLE auction_images (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  auction_id uuid NOT NULL REFERENCES auctions(id),
  url text NOT NULL,
  order_index int DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- MESSAGES
CREATE TABLE messages (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  auction_id uuid REFERENCES auctions(id),
  from_user uuid NOT NULL REFERENCES users(id),
  to_user uuid REFERENCES users(id),
  content text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- AUDIT LOGS
CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  actor_user_id uuid REFERENCES users(id),
  action varchar(100),
  entity varchar(50),
  entity_id uuid,
  payload jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);