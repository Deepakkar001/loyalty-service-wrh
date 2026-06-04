-- Manual schema change (Flyway not used). Phase 2: purchase progress for time-window milestones.
-- Run once; ignore error if column already exists.
ALTER TABLE referrals
  ADD COLUMN progress_json JSON NULL AFTER completed_stages_json;
