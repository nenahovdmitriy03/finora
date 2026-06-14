-- ============================================================================
-- Finora · Supabase schema
-- Run this in Supabase Dashboard → SQL Editor → New Query → Run
-- ============================================================================

-- 1. Accounts
create table public.accounts (
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    name        text not null,
    type        text not null default 'CARD',
    initial_balance double precision not null default 0,
    color       bigint not null default 4285542375,
    icon_key    text not null default 'wallet',
    interest_rate       double precision not null default 0,
    interest_period     text,
    last_interest_at    bigint,
    interest_payout_minute integer not null default 540,
    interest_payout_day    integer not null default 1,
    created_at  bigint not null,
    updated_at  timestamptz not null default now()
);
alter table public.accounts enable row level security;
create policy "Users see own accounts"   on public.accounts for select using (auth.uid() = user_id);
create policy "Users insert own accounts" on public.accounts for insert with check (auth.uid() = user_id);
create policy "Users update own accounts" on public.accounts for update using (auth.uid() = user_id);
create policy "Users delete own accounts" on public.accounts for delete using (auth.uid() = user_id);

-- 2. Categories
create table public.categories (
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    name        text not null,
    type        text not null,
    icon_key    text not null default 'category',
    color       bigint not null default 4285542375,
    is_default  boolean not null default false,
    updated_at  timestamptz not null default now()
);
alter table public.categories enable row level security;
create policy "Users see own categories"   on public.categories for select using (auth.uid() = user_id);
create policy "Users insert own categories" on public.categories for insert with check (auth.uid() = user_id);
create policy "Users update own categories" on public.categories for update using (auth.uid() = user_id);
create policy "Users delete own categories" on public.categories for delete using (auth.uid() = user_id);

-- 3. Transactions
create table public.transactions (
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    amount      double precision not null,
    type        text not null,
    account_id  uuid not null references public.accounts(id) on delete cascade,
    category_id uuid references public.categories(id) on delete set null,
    note        text not null default '',
    date        bigint not null,
    created_at  bigint not null,
    updated_at  timestamptz not null default now()
);
create index idx_transactions_account on public.transactions(account_id);
create index idx_transactions_date on public.transactions(date);
alter table public.transactions enable row level security;
create policy "Users see own transactions"   on public.transactions for select using (auth.uid() = user_id);
create policy "Users insert own transactions" on public.transactions for insert with check (auth.uid() = user_id);
create policy "Users update own transactions" on public.transactions for update using (auth.uid() = user_id);
create policy "Users delete own transactions" on public.transactions for delete using (auth.uid() = user_id);

-- 4. Goals
create table public.goals (
    id            uuid default gen_random_uuid() primary key,
    user_id       uuid not null references auth.users(id) on delete cascade,
    name          text not null,
    target_amount double precision not null,
    saved_amount  double precision not null default 0,
    icon_key      text not null default 'target',
    color         bigint not null default 4282298252,
    deadline      bigint,
    created_at    bigint not null,
    updated_at    timestamptz not null default now()
);
alter table public.goals enable row level security;
create policy "Users see own goals"   on public.goals for select using (auth.uid() = user_id);
create policy "Users insert own goals" on public.goals for insert with check (auth.uid() = user_id);
create policy "Users update own goals" on public.goals for update using (auth.uid() = user_id);
create policy "Users delete own goals" on public.goals for delete using (auth.uid() = user_id);

-- 5. Goal Contributions
create table public.goal_contributions (
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    goal_id     uuid not null references public.goals(id) on delete cascade,
    account_id  uuid not null references public.accounts(id) on delete cascade,
    amount      double precision not null,
    date        bigint not null
);
create index idx_goal_contributions_goal on public.goal_contributions(goal_id);
create index idx_goal_contributions_account on public.goal_contributions(account_id);
alter table public.goal_contributions enable row level security;
create policy "Users see own goal_contributions"   on public.goal_contributions for select using (auth.uid() = user_id);
create policy "Users insert own goal_contributions" on public.goal_contributions for insert with check (auth.uid() = user_id);
create policy "Users update own goal_contributions" on public.goal_contributions for update using (auth.uid() = user_id);
create policy "Users delete own goal_contributions" on public.goal_contributions for delete using (auth.uid() = user_id);

-- 6. Transfers
create table public.transfers (
    id              uuid default gen_random_uuid() primary key,
    user_id         uuid not null references auth.users(id) on delete cascade,
    from_account_id uuid not null references public.accounts(id) on delete cascade,
    to_account_id   uuid not null references public.accounts(id) on delete cascade,
    amount          double precision not null,
    note            text not null default '',
    date            bigint not null,
    created_at      bigint not null,
    updated_at      timestamptz not null default now()
);
create index idx_transfers_from on public.transfers(from_account_id);
create index idx_transfers_to on public.transfers(to_account_id);
create index idx_transfers_date on public.transfers(date);
alter table public.transfers enable row level security;
create policy "Users see own transfers"   on public.transfers for select using (auth.uid() = user_id);
create policy "Users insert own transfers" on public.transfers for insert with check (auth.uid() = user_id);
create policy "Users update own transfers" on public.transfers for update using (auth.uid() = user_id);
create policy "Users delete own transfers" on public.transfers for delete using (auth.uid() = user_id);

-- 7. Recurring Rules
create table public.recurring_rules (
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
create index idx_recurring_rules_category on public.recurring_rules(category_id);
create index idx_recurring_rules_account on public.recurring_rules(account_id);
alter table public.recurring_rules enable row level security;
create policy "Users see own recurring_rules"   on public.recurring_rules for select using (auth.uid() = user_id);
create policy "Users insert own recurring_rules" on public.recurring_rules for insert with check (auth.uid() = user_id);
create policy "Users update own recurring_rules" on public.recurring_rules for update using (auth.uid() = user_id);
create policy "Users delete own recurring_rules" on public.recurring_rules for delete using (auth.uid() = user_id);

-- 8. Budgets
create table public.budgets (
    id           uuid default gen_random_uuid() primary key,
    user_id      uuid not null references auth.users(id) on delete cascade,
    category_id  uuid not null references public.categories(id) on delete cascade,
    limit_amount double precision not null,
    period_days  integer not null default 30,
    created_at   bigint not null,
    updated_at   timestamptz not null default now()
);
create index idx_budgets_category on public.budgets(category_id);
alter table public.budgets enable row level security;
create policy "Users see own budgets"   on public.budgets for select using (auth.uid() = user_id);
create policy "Users insert own budgets" on public.budgets for insert with check (auth.uid() = user_id);
create policy "Users update own budgets" on public.budgets for update using (auth.uid() = user_id);
create policy "Users delete own budgets" on public.budgets for delete using (auth.uid() = user_id);

-- 9. Templates
create table public.templates (
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
create index idx_templates_category on public.templates(category_id);
create index idx_templates_account on public.templates(account_id);
alter table public.templates enable row level security;
create policy "Users see own templates"   on public.templates for select using (auth.uid() = user_id);
create policy "Users insert own templates" on public.templates for insert with check (auth.uid() = user_id);
create policy "Users update own templates" on public.templates for update using (auth.uid() = user_id);
create policy "Users delete own templates" on public.templates for delete using (auth.uid() = user_id);

-- 10. Tags
create table public.tags (
    id         uuid default gen_random_uuid() primary key,
    user_id    uuid not null references auth.users(id) on delete cascade,
    name       text not null,
    color      bigint not null,
    updated_at timestamptz not null default now()
);
alter table public.tags enable row level security;
create policy "Users see own tags"   on public.tags for select using (auth.uid() = user_id);
create policy "Users insert own tags" on public.tags for insert with check (auth.uid() = user_id);
create policy "Users update own tags" on public.tags for update using (auth.uid() = user_id);
create policy "Users delete own tags" on public.tags for delete using (auth.uid() = user_id);

-- 11. Transaction Tags
create table public.transaction_tags (
    user_id        uuid not null references auth.users(id) on delete cascade,
    transaction_id uuid not null references public.transactions(id) on delete cascade,
    tag_id         uuid not null references public.tags(id) on delete cascade,
    primary key (transaction_id, tag_id)
);
create index idx_transaction_tags_transaction on public.transaction_tags(transaction_id);
create index idx_transaction_tags_tag on public.transaction_tags(tag_id);
alter table public.transaction_tags enable row level security;
create policy "Users see own transaction_tags"   on public.transaction_tags for select using (auth.uid() = user_id);
create policy "Users insert own transaction_tags" on public.transaction_tags for insert with check (auth.uid() = user_id);
create policy "Users update own transaction_tags" on public.transaction_tags for update using (auth.uid() = user_id);
create policy "Users delete own transaction_tags" on public.transaction_tags for delete using (auth.uid() = user_id);

-- 12. Challenges
create table public.challenges (
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
create index idx_challenges_category on public.challenges(category_id);
alter table public.challenges enable row level security;
create policy "Users see own challenges"   on public.challenges for select using (auth.uid() = user_id);
create policy "Users insert own challenges" on public.challenges for insert with check (auth.uid() = user_id);
create policy "Users update own challenges" on public.challenges for update using (auth.uid() = user_id);
create policy "Users delete own challenges" on public.challenges for delete using (auth.uid() = user_id);

-- 13. Auto-update updated_at trigger
create or replace function public.set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create trigger accounts_updated_at before update on public.accounts for each row execute function public.set_updated_at();
create trigger categories_updated_at before update on public.categories for each row execute function public.set_updated_at();
create trigger transactions_updated_at before update on public.transactions for each row execute function public.set_updated_at();
create trigger goals_updated_at before update on public.goals for each row execute function public.set_updated_at();
create trigger transfers_updated_at before update on public.transfers for each row execute function public.set_updated_at();
create trigger recurring_rules_updated_at before update on public.recurring_rules for each row execute function public.set_updated_at();
create trigger budgets_updated_at before update on public.budgets for each row execute function public.set_updated_at();
create trigger templates_updated_at before update on public.templates for each row execute function public.set_updated_at();
create trigger tags_updated_at before update on public.tags for each row execute function public.set_updated_at();
create trigger challenges_updated_at before update on public.challenges for each row execute function public.set_updated_at();
