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

-- 7. Auto-update updated_at trigger
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
