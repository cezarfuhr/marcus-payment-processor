-- Seed data for testing
-- This file contains sample data for development and testing

-- Clear existing data (for development only)
TRUNCATE TABLE audit_log, payment_queue, payments CASCADE;

-- Sample payments
INSERT INTO payments (id, payment_id, idempotency_key, type, amount, currency, status,
                      sender_document, sender_bank_code, sender_account,
                      receiver_pix_key, receiver_pix_key_type,
                      created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'PAY-2025-000001', uuid_generate_v4(), 'PIX', 150.00, 'BRL', 'SUCCESS',
     '12345678900', '001', '12345-6', 'recipient@example.com', 'EMAIL',
     CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour'),

    (uuid_generate_v4(), 'PAY-2025-000002', uuid_generate_v4(), 'PIX', 250.50, 'BRL', 'PENDING',
     '98765432100', '237', '98765-4', '+5511987654321', 'PHONE',
     CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),

    (uuid_generate_v4(), 'PAY-2025-000003', uuid_generate_v4(), 'PIX', 1000.00, 'BRL', 'PROCESSING',
     '11122233344', '341', '11111-1', '12345678900', 'CPF',
     CURRENT_TIMESTAMP - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes'),

    (uuid_generate_v4(), 'PAY-2025-000004', uuid_generate_v4(), 'PIX', 75.25, 'BRL', 'FAILED',
     '55566677788', '104', '22222-2', 'invalid@example.com', 'EMAIL',
     CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours');

-- Note: You can add more sample data as needed for testing
