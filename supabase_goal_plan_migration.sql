-- ============================================================================
-- Finora goal savings plan migration
-- Run once in Supabase Dashboard -> SQL Editor for an existing Finora project.
-- Adds saved per-goal plan settings.
-- ============================================================================

alter table public.goals
    add column if not exists plan_months integer;

alter table public.goals
    add column if not exists planned_monthly_amount double precision;
