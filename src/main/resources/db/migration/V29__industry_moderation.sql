-- Industry (business category) moderation: tenant-typed "Other" suggestions must be
-- reviewed by an admin before they appear in the public dropdown.
--
-- Existing seeded rows default to APPROVED so behaviour for them is identical.

ALTER TABLE ref_business_category
    ADD COLUMN status                  VARCHAR(16)  NOT NULL DEFAULT 'APPROVED'
        AFTER is_active,
    ADD COLUMN submitted_by_tenant_id  VARCHAR(36)  NULL
        AFTER status,
    ADD COLUMN submitted_label         VARCHAR(150) NULL
        AFTER submitted_by_tenant_id,
    ADD COLUMN decision_reason         VARCHAR(500) NULL
        AFTER submitted_label,
    ADD COLUMN decided_by_admin_id     VARCHAR(36)  NULL
        AFTER decision_reason,
    ADD COLUMN decided_at              DATETIME(6)  NULL
        AFTER decided_by_admin_id,
    ADD INDEX idx_ref_business_category_status (status, is_active, sort_order);
