-- Rows inserted via JDBC without added_at may have zero dates; Hibernate cannot read them.
UPDATE campaign_target_customers
SET added_at = CURRENT_TIMESTAMP(6)
WHERE added_at IS NULL
   OR added_at = '0000-00-00 00:00:00'
   OR added_at < '1971-01-01';
