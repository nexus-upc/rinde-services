-- Comprobantes con archivo guardado. Las filas anteriores solo tienen image_url y no tienen storage_key.
ALTER TABLE expense.evidences ALTER COLUMN image_url DROP NOT NULL;
ALTER TABLE expense.evidences ADD COLUMN file_name VARCHAR(255);
ALTER TABLE expense.evidences ADD COLUMN content_type VARCHAR(100);
ALTER TABLE expense.evidences ADD COLUMN sha256 VARCHAR(64);
ALTER TABLE expense.evidences ADD COLUMN storage_key VARCHAR(255);
ALTER TABLE expense.evidences ADD COLUMN uploaded_by UUID;
