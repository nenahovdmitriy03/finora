-- ============================================================================
-- Finora cloud sync v7 migration
-- Run once in Supabase Dashboard -> SQL Editor for an existing Finora project.
-- Adds cloud tables for Room v7 entities that were previously local-only.
-- ============================================================================

create table if not exists public.recurring_rules (
    id               uuid default gen_random_uuid() primary key,
    user_id          uuid not null references auth.users(id) on delete cascade,
    name             text not null,
    amount           double precision not null,
    type             text not null,
    category_id      uuid not null references public.categories(id) on delete cascade,
    account_id       uuid not null references public.accounts(id) on delete cascade,
    period_days      integer not null,
    last_executed_at bigint,
    created_at       bigint not null,
    enabled          boolean not null default true,
    updated_at       timestamptz not null default now()
);
create index if not exists idx_recurring_rules_category on public.recurring_rules(category_id);
create index if not exists idx_recurring_rules_account on public.recurring_rules(account_id);
alter table public.recurring_rules enable row level security;
drop policy if exists "Users see own recurring_rules" on public.recurring_rules;
drop policy if exists "Users insert own recurring_rules" on public.recurring_rules;
drop policy if exists "Users update own recurring_rules" on public.recurring_rules;
drop policy if exists "Users delete own recurring_rules" on public.recurring_rules;
create policy "Users see own recurring_rules" on public.recurring_rules for select using (auth.uid() = user_id);
create policy "Users insert own recurring_rules" on public.recurring_rules for insert with check (auth.uid() = user_id);
create policy "Users update own recurring_rules" on public.recurring_rules for update using (auth.uid() = user_id);
create policy "Users delete own recurring_rules" on public.recurring_rules for delete using (auth.uid() = user_id);

create table if not exists public.budgets (
    id           uuid default gen_random_uuid() primary key,
    user_id      uuid not null references auth.users(id) on delete cascade,
    category_id  uuid not null references public.categories(id) on delete cascade,
    limit_amount double precision not null,
    period_days  integer not null default 30,
    created_at   bigint not null,
    updated_at   timestamptz not null default now()
);
create index if not exists idx_budgets_category on public.budgets(category_id);
alter table public.budgets enable row level security;
drop policy if exists "Users see own budgets" on public.budgets;
drop policy if exists "Users insert own budgets" on public.budgets;
drop policy if exists "Users update own budgets" on public.budgets;
drop policy if exists "Users delete own budgets" on public.budgets;
create policy "Users see own budgets" on public.budgets for select using (auth.uid() = user_id);
create policy "Users insert own budgets" on public.budgets for insert with check (auth.uid() = user_id);
create policy "Users update own budgets" on public.budgets for update using (auth.uid() = user_id);
create policy "Users delete own budgets" on public.budgets for delete using (auth.uid() = user_id);

create table if not exists public.templates (
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    name        text not null,
    amount      double precision not null,
    type        text not null,
    category_id uuid references public.categories(id) on delete set null,
    account_id  uuid references public.accounts(id) on delete set null,
    note        text not null default '',
    created_at  bigint not null,
    updated_at  timestamptz not null default now()
);
create index if not exists idx_templates_category on public.templates(category_id);
create index if not exists idx_templates_account on public.templates(account_id);
alter table public.templates enable row level security;
drop policy if exists "Users see own templates" on public.templates;
drop policy if exists "Users insert own templates" on public.templates;
drop policy if exists "Users update own templates" on public.templates;
drop policy if exists "Users delete own templates" on public.templates;
create policy "Users see own templates" on public.templates for select using (auth.uid() = user_id);
create policy "Users insert own templates" on public.templates for insert with check (auth.uid() = user_id);
create policy "Users update own templates" on public.templates for update using (auth.uid() = user_id);
create policy "Users delete own templates" on public.templates for delete using (auth.uid() = user_id);

create table if not exists public.tags (
    id         uuid default gen_random_uuid() primary key,
    user_id    uuid not null references auth.users(id) on delete cascade,
    name       text not null,
    color      bigint not null,
    updated_at timestamptz not null default now()
);
alter table public.tags enable row level security;
drop policy if exists "Users see own tags" on public.tags;
drop policy if exists "Users insert own tags" on public.tags;
drop policy if exists "Users update own tags" on public.tags;
drop policy if exists "Users delete own tags" on public.tags;
create policy "Users see own tags" on public.tags for select using (auth.uid() = user_id);
create policy "Users insert own tags" on public.tags for insert with check (auth.uid() = user_id);
create policy "Users update own tags" on public.tags for update using (auth.uid() = user_id);
create policy "Users delete own tags" on public.tags for delete using (auth.uid() = user_id);

create table if not exists public.transaction_tags (
    user_id        uuid not null references auth.users(id) on delete cascade,
    transaction_id uuid not null references public.transactions(id) on delete cascade,
    tag_id         uuid not null references public.tags(id) on delete cascade,
    primary key (transaction_id, tag_id)
);
create index if not exists idx_transaction_tags_transaction on public.transaction_tags(transaction_id);
create index if not exists idx_transaction_tags_tag on public.transaction_tags(tag_id);
alter table public.transaction_tags enable row level security;
drop policy if exists "Users see own transaction_tags" on public.transaction_tags;
drop policy if exists "Users insert own transaction_tags" on public.transaction_tags;
drop policy if exists "Users update own transaction_tags" on public.transaction_tags;
drop policy if exists "Users delete own transaction_tags" on public.transaction_tags;
create policy "Users see own transaction_tags" on public.transaction_tags for select using (auth.uid() = user_id);
create policy "Users insert own transaction_tags" on public.transaction_tags for insert with check (auth.uid() = user_id);
create policy "Users update own transaction_tags" on public.transaction_tags for update using (auth.uid() = user_id);
create policy "Users delete own transaction_tags" on public.transaction_tags for delete using (auth.uid() = user_id);

create table if not exists public.challenges (
    id            uuid default gen_random_uuid() primary key,
    user_id       uuid not null references auth.users(id) on delete cascade,
    title         text not null,
    description   text not null,
    emoji         text not null default 'target',
    target_days   integer not null,
    target_amount double precision,
    category_id   uuid references public.categories(id) on delete set null,
    start_date    bigint not null,
    end_date      bigint not null,
    completed     boolean not null default false,
    created_at    bigint not null,
    updated_at    timestamptz not null default now()
);
create index if not exists idx_challenges_category on public.challenges(category_id);
alter table public.challenges enable row level security;
drop policy if exists "Users see own challenges" on public.challenges;
drop policy if exists "Users insert own challenges" on public.challenges;
drop policy if exists "Users update own challenges" on public.challenges;
drop policy if exists "Users delete own challenges" on public.challenges;
create policy "Users see own challenges" on public.challenges for select using (auth.uid() = user_id);
create policy "Users insert own challenges" on public.challenges for insert with check (auth.uid() = user_id);
create policy "Users update own challenges" on public.challenges for update using (auth.uid() = user_id);
create policy "Users delete own challenges" on public.challenges for delete using (auth.uid() = user_id);

create or replace function public.set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

drop trigger if exists recurring_rules_updated_at on public.recurring_rules;
drop trigger if exists budgets_updated_at on public.budgets;
drop trigger if exists templates_updated_at on public.templates;
drop trigger if exists tags_updated_at on public.tags;
drop trigger if exists challenges_updated_at on public.challenges;
create trigger recurring_rules_updated_at before update on public.recurring_rules for each row execute function public.set_updated_at();
create trigger budgets_updated_at before update on public.budgets for each row execute function public.set_updated_at();
create trigger templates_updated_at before update on public.templates for each row execute function public.set_updated_at();
create trigger tags_updated_at before update on public.tags for each row execute function public.set_updated_at();
create trigger challenges_updated_at before update on public.challenges for each row execute function public.set_updated_at();
