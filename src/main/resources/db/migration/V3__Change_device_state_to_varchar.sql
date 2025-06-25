-- Change device state from enum to varchar for R2DBC compatibility
-- First, convert existing enum values to varchar
ALTER TABLE devices ALTER COLUMN state TYPE VARCHAR(20) USING state::VARCHAR;

-- Drop the enum type if no other tables are using it
DROP TYPE device_state;
