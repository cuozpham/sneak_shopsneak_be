CREATE TABLE IF NOT EXISTS vqr_transaction_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(255) NULL,
    amount DECIMAL(19,2) NULL,
    bank_account VARCHAR(255) NULL,
    transaction_date VARCHAR(255) NULL,
    raw_payload LONGTEXT NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
