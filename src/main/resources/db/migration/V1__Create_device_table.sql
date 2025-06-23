-- Create device state enum type
CREATE TYPE device_state AS ENUM ('available', 'in-use', 'inactive');

-- Create devices table
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    brand TEXT NOT NULL,
    state device_state NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create index on state for efficient filtering
CREATE INDEX idx_devices_state ON devices(state);

-- Create index on created_at for efficient sorting
CREATE INDEX idx_devices_created_at ON devices(created_at);
