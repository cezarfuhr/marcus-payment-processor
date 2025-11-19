-- Open Finance Payment Processor Database Schema
-- PostgreSQL 15+

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id VARCHAR(50) UNIQUE NOT NULL,
    idempotency_key UUID UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PIX', 'TED', 'BOLETO')),
    amount DECIMAL(15,2) NOT NULL CHECK (amount >= 0.01 AND amount <= 10000.00),
    currency VARCHAR(3) DEFAULT 'BRL',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    sender_document VARCHAR(14),
    sender_bank_code VARCHAR(3),
    sender_account VARCHAR(20),
    receiver_pix_key VARCHAR(255),
    receiver_pix_key_type VARCHAR(20),
    confirmation_code VARCHAR(255),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for payments
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_payment_id ON payments(payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency_key ON payments(idempotency_key);

-- Payment queue table
CREATE TABLE IF NOT EXISTS payment_queue (
    id BIGSERIAL PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    retry_count INT DEFAULT 0 NOT NULL,
    max_retries INT DEFAULT 3 NOT NULL,
    next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for payment_queue
CREATE INDEX IF NOT EXISTS idx_payment_queue_next_retry ON payment_queue(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_payment_queue_payment_id ON payment_queue(payment_id);

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('CREATED', 'STATUS_CHANGED', 'RECONCILED', 'RETRY_ATTEMPTED', 'FAILED')),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit_log
CREATE INDEX IF NOT EXISTS idx_audit_log_payment_id ON audit_log(payment_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_type ON audit_log(event_type);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE payments IS 'Stores all payment transactions';
COMMENT ON TABLE payment_queue IS 'Queue for async payment processing with retry logic';
COMMENT ON TABLE audit_log IS 'Immutable audit trail of all payment events';

COMMENT ON COLUMN payments.idempotency_key IS 'Unique key to prevent duplicate payments';
COMMENT ON COLUMN payment_queue.next_retry_at IS 'Next time this payment should be retried';
COMMENT ON COLUMN audit_log.metadata IS 'Additional context stored as JSON';
