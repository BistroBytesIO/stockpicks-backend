-- Stock Picks Database Setup Script
-- Run this script as PostgreSQL superuser to create database and user

-- Create database
CREATE DATABASE stockpicks;

-- Create user
CREATE USER stockpicks_user WITH PASSWORD 'your_password_here';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE stockpicks TO stockpicks_user;

-- Connect to the database
\c stockpicks

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO stockpicks_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO stockpicks_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO stockpicks_user;

-- Sample subscription plans data
INSERT INTO subscription_plans (name, description, price, duration_months, stripe_price_id, active) VALUES
('Monthly Plan', 'Get access to all stock picks with monthly billing', 29.99, 1, 'price_monthly', true),
('6-Month Plan', 'Get access to all stock picks with 6-month billing', 159.99, 6, 'price_6month', true),
('Annual Plan', 'Best value - Get access to all stock picks with annual billing', 299.99, 12, 'price_annual', true);

-- Create indexes for better performance
CREATE INDEX idx_stock_picks_symbol ON stock_picks(symbol);
CREATE INDEX idx_stock_picks_pick_date ON stock_picks(pick_date);
CREATE INDEX idx_stock_picks_pick_type ON stock_picks(pick_type);
CREATE INDEX idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);