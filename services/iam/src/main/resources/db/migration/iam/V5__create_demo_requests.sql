CREATE TABLE iam.demo_requests (
    id UUID PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    fleet_size INTEGER NOT NULL CHECK (fleet_size BETWEEN 1 AND 500),
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_demo_requests_received ON iam.demo_requests (received_at DESC, id);
CREATE INDEX idx_demo_requests_email ON iam.demo_requests (email, received_at);
