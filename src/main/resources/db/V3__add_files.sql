-- Add image_path column to mensagens table
ALTER TABLE mensagens ADD COLUMN IF NOT EXISTS image_path VARCHAR(255);

-- Add image_path column to chamados table
ALTER TABLE chamados ADD COLUMN IF NOT EXISTS image_path VARCHAR(255);