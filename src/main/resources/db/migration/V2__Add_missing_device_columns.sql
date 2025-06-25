-- Add missing columns to devices table
ALTER TABLE devices ADD COLUMN type TEXT NOT NULL DEFAULT 'Unknown';
ALTER TABLE devices ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Create index on brand for efficient filtering
CREATE INDEX idx_devices_brand ON devices(brand);

-- Create index on type for efficient filtering  
CREATE INDEX idx_devices_type ON devices(type);

-- Remove default constraint after adding the column
ALTER TABLE devices ALTER COLUMN type DROP DEFAULT;
