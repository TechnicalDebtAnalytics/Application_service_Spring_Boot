ALTER TABLE class_metrics
    DROP COLUMN IF EXISTS loc,
    DROP COLUMN IF EXISTS cyclomatic_complexity;
