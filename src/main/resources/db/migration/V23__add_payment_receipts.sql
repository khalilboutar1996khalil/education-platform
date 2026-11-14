-- A student may attach the receipt of their payment (a photo or a PDF) to their access request.
-- The form is public, so that file has no uploader: stored_files.uploaded_by_id becomes optional.
ALTER TABLE stored_files ALTER COLUMN uploaded_by_id DROP NOT NULL;

ALTER TABLE access_requests
    ADD COLUMN receipt_file_id BIGINT,
    ADD CONSTRAINT fk_access_requests_receipt FOREIGN KEY (receipt_file_id) REFERENCES stored_files (id);

-- With a receipt attached the typed reference is optional: the service asks for one or the other.
ALTER TABLE access_requests DROP CONSTRAINT ck_access_requests_payment;
ALTER TABLE access_requests ADD CONSTRAINT ck_access_requests_payment CHECK (
    (plan_id IS NULL AND payment_method IS NULL AND receipt_file_id IS NULL)
    OR (plan_id IS NOT NULL AND price_dt IS NOT NULL AND duration_days IS NOT NULL
        AND payment_method IS NOT NULL
        AND (payment_reference IS NOT NULL OR receipt_file_id IS NOT NULL))
);
