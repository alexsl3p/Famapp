-- ============================================================
-- Kinly / Family OS — Idempotent Migration
-- Safe to run multiple times. Handles existing tables/columns.
-- ============================================================

-- Extensions
create extension if not exists "pgcrypto";

-- ============================================================
-- TABLES (all with IF NOT EXISTS)
-- ============================================================

create table if not exists public.families (
  id          uuid        primary key default gen_random_uuid(),
  name        text        not null,
  invite_code text        unique not null default upper(substring(encode(gen_random_bytes(4), 'hex'), 1, 6)),
  created_by  uuid        references auth.users(id) on delete set null,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

-- profiles: may already exist; just add the missing column
create table if not exists public.profiles (
  id               uuid        primary key references auth.users(id) on delete cascade,
  full_name        text,
  avatar_url       text,
  active_family_id uuid,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);
alter table public.profiles add column if not exists active_family_id uuid references public.families(id) on delete set null;
alter table public.profiles add column if not exists full_name text;
alter table public.profiles add column if not exists avatar_url text;
alter table public.profiles add column if not exists created_at timestamptz not null default now();
alter table public.profiles add column if not exists updated_at timestamptz not null default now();

create table if not exists public.family_members (
  id        uuid        primary key default gen_random_uuid(),
  family_id uuid        not null references public.families(id) on delete cascade,
  user_id   uuid        not null references auth.users(id) on delete cascade,
  nickname  text,
  color     text        default 'purple',
  role      text        not null default 'member',
  joined_at timestamptz not null default now(),
  unique(family_id, user_id)
);

create table if not exists public.tasks (
  id           uuid        primary key default gen_random_uuid(),
  family_id    uuid        not null references public.families(id) on delete cascade,
  title        text        not null,
  description  text,
  assigned_to  uuid        references auth.users(id) on delete set null,
  created_by   uuid        not null references auth.users(id) on delete cascade,
  due_date     date,
  due_time     time,
  repeat_type  text        not null default 'none',
  is_completed boolean     not null default false,
  completed_by uuid        references auth.users(id) on delete set null,
  completed_at timestamptz,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table if not exists public.shopping_lists (
  id         uuid        primary key default gen_random_uuid(),
  family_id  uuid        not null references public.families(id) on delete cascade,
  title      text        not null default 'Продукты',
  created_by uuid        not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shopping_items (
  id              uuid        primary key default gen_random_uuid(),
  family_id       uuid        not null references public.families(id) on delete cascade,
  list_id         uuid        not null references public.shopping_lists(id) on delete cascade,
  title           text        not null,
  quantity        text,
  unit            text,
  estimated_price double precision,
  created_by      uuid        not null references auth.users(id) on delete cascade,
  is_checked      boolean     not null default false,
  checked_by      uuid        references auth.users(id) on delete set null,
  checked_at      timestamptz,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

-- ============================================================
-- UPDATED_AT TRIGGER FUNCTION
-- ============================================================
create or replace function public.handle_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists set_families_updated_at       on public.families;
drop trigger if exists set_profiles_updated_at       on public.profiles;
drop trigger if exists set_tasks_updated_at          on public.tasks;
drop trigger if exists set_shopping_lists_updated_at on public.shopping_lists;
drop trigger if exists set_shopping_items_updated_at on public.shopping_items;

create trigger set_families_updated_at       before update on public.families       for each row execute function public.handle_updated_at();
create trigger set_profiles_updated_at       before update on public.profiles        for each row execute function public.handle_updated_at();
create trigger set_tasks_updated_at          before update on public.tasks           for each row execute function public.handle_updated_at();
create trigger set_shopping_lists_updated_at before update on public.shopping_lists  for each row execute function public.handle_updated_at();
create trigger set_shopping_items_updated_at before update on public.shopping_items  for each row execute function public.handle_updated_at();

-- ============================================================
-- AUTO-CREATE PROFILE TRIGGER
-- ============================================================
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name, avatar_url)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name'),
    new.raw_user_meta_data->>'avatar_url'
  )
  on conflict (id) do update
    set full_name  = coalesce(excluded.full_name, public.profiles.full_name),
        avatar_url = coalesce(excluded.avatar_url, public.profiles.avatar_url);
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ============================================================
-- ROW LEVEL SECURITY
-- ============================================================
alter table public.profiles       enable row level security;
alter table public.families       enable row level security;
alter table public.family_members enable row level security;
alter table public.tasks          enable row level security;
alter table public.shopping_lists enable row level security;
alter table public.shopping_items enable row level security;

-- Helper function
create or replace function public.is_family_member(p_family_id uuid)
returns boolean language sql security definer as $$
  select exists (
    select 1 from public.family_members
    where family_id = p_family_id and user_id = auth.uid()
  );
$$;

-- Policies (idempotent via DO blocks)
do $$ begin
  create policy "Users can read own profile" on public.profiles for select using (id = auth.uid());
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Users can read family members profiles" on public.profiles for select using (
    exists (
      select 1 from public.family_members fm1
      join public.family_members fm2 on fm1.family_id = fm2.family_id
      where fm1.user_id = auth.uid() and fm2.user_id = profiles.id
    )
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Users can update own profile" on public.profiles for update using (id = auth.uid());
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Members can read their family" on public.families for select using (public.is_family_member(id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Creators can update their family" on public.families for update using (created_by = auth.uid());
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Members can see members of their families" on public.family_members for select using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can read tasks" on public.tasks for select using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can insert tasks" on public.tasks for insert with check (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can update tasks" on public.tasks for update using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can delete tasks" on public.tasks for delete using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can read shopping lists" on public.shopping_lists for select using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can insert shopping lists" on public.shopping_lists for insert with check (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can update shopping lists" on public.shopping_lists for update using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can delete shopping lists" on public.shopping_lists for delete using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can read shopping items" on public.shopping_items for select using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can insert shopping items" on public.shopping_items for insert with check (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can update shopping items" on public.shopping_items for update using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "Family members can delete shopping items" on public.shopping_items for delete using (public.is_family_member(family_id));
exception when duplicate_object then null; end $$;

-- ============================================================
-- RPC FUNCTIONS
-- ============================================================

create or replace function public.create_family_with_defaults(p_name text)
returns public.families language plpgsql security definer set search_path = public as $$
declare
  v_user_id uuid := auth.uid();
  v_family  public.families;
begin
  if v_user_id is null then
    raise exception 'Not authenticated';
  end if;

  insert into public.families (name, created_by)
  values (p_name, v_user_id)
  returning * into v_family;

  insert into public.family_members (family_id, user_id, role, nickname)
  values (v_family.id, v_user_id, 'owner',
    (select full_name from public.profiles where id = v_user_id));

  update public.profiles set active_family_id = v_family.id where id = v_user_id;

  insert into public.shopping_lists (family_id, title, created_by)
  values (v_family.id, 'Продукты', v_user_id);

  return v_family;
end;
$$;

create or replace function public.join_family_by_code(p_code text)
returns void language plpgsql security definer set search_path = public as $$
declare
  v_user_id  uuid := auth.uid();
  v_family   public.families;
begin
  if v_user_id is null then
    raise exception 'Not authenticated';
  end if;

  select * into v_family from public.families
  where upper(invite_code) = upper(p_code);

  if v_family.id is null then
    raise exception 'Invalid invite code';
  end if;

  if exists (
    select 1 from public.family_members
    where family_id = v_family.id and user_id = v_user_id
  ) then
    update public.profiles set active_family_id = v_family.id where id = v_user_id;
    return;
  end if;

  insert into public.family_members (family_id, user_id, role, nickname)
  values (v_family.id, v_user_id, 'member',
    (select full_name from public.profiles where id = v_user_id));

  update public.profiles set active_family_id = v_family.id where id = v_user_id;
end;
$$;

create or replace function public.leave_family(p_family_id uuid)
returns void language plpgsql security definer set search_path = public as $$
declare
  v_user_id uuid := auth.uid();
begin
  delete from public.family_members
  where family_id = p_family_id and user_id = v_user_id;

  update public.profiles
  set active_family_id = null
  where id = v_user_id and active_family_id = p_family_id;
end;
$$;

create or replace function public.regenerate_invite_code(p_family_id uuid)
returns text language plpgsql security definer set search_path = public as $$
declare
  v_new_code text;
begin
  if not public.is_family_member(p_family_id) then
    raise exception 'Not a member of this family';
  end if;

  v_new_code := upper(substring(encode(gen_random_bytes(4), 'hex'), 1, 6));
  update public.families set invite_code = v_new_code where id = p_family_id;
  return v_new_code;
end;
$$;

create or replace function public.create_task(
  p_family_id  uuid,
  p_title      text,
  p_description text default null,
  p_assigned_to uuid default null,
  p_due_date   date default null,
  p_repeat_type text default 'none',
  p_created_by  uuid default null
) returns void language plpgsql security definer set search_path = public as $$
declare
  v_user_id uuid := coalesce(p_created_by, auth.uid());
begin
  if not public.is_family_member(p_family_id) then
    raise exception 'Not a member of this family';
  end if;

  insert into public.tasks (
    family_id, title, description, assigned_to, created_by, due_date, repeat_type
  ) values (
    p_family_id, p_title, p_description, p_assigned_to, v_user_id, p_due_date, p_repeat_type
  );
end;
$$;

create or replace function public.complete_task(p_task_id uuid)
returns json language plpgsql security definer set search_path = public as $$
declare
  v_user_id  uuid := auth.uid();
  v_task     public.tasks;
  v_new_date date;
begin
  select * into v_task from public.tasks where id = p_task_id;

  if v_task.id is null then
    raise exception 'Task not found';
  end if;

  if not public.is_family_member(v_task.family_id) then
    raise exception 'Not a member of this family';
  end if;

  update public.tasks
  set is_completed = true, completed_by = v_user_id, completed_at = now()
  where id = p_task_id;

  if v_task.repeat_type != 'none' and v_task.due_date is not null then
    v_new_date := case v_task.repeat_type
      when 'daily'   then v_task.due_date + interval '1 day'
      when 'weekly'  then v_task.due_date + interval '7 days'
      when 'monthly' then v_task.due_date + interval '1 month'
      else null
    end;

    if v_new_date is not null then
      insert into public.tasks (
        family_id, title, description, assigned_to, created_by,
        due_date, due_time, repeat_type
      ) values (
        v_task.family_id, v_task.title, v_task.description, v_task.assigned_to,
        v_task.created_by, v_new_date, v_task.due_time, v_task.repeat_type
      );

      return json_build_object('success', true, 'rolled_forward', true, 'new_due_date', v_new_date::text);
    end if;
  end if;

  return json_build_object('success', true, 'rolled_forward', false, 'new_due_date', null);
end;
$$;

create or replace function public.uncomplete_task(p_task_id uuid)
returns void language plpgsql security definer set search_path = public as $$
declare
  v_task public.tasks;
begin
  select * into v_task from public.tasks where id = p_task_id;
  if v_task.id is null then return; end if;

  if not public.is_family_member(v_task.family_id) then
    raise exception 'Not a member of this family';
  end if;

  update public.tasks
  set is_completed = false, completed_by = null, completed_at = null
  where id = p_task_id;
end;
$$;

create or replace function public.mark_shopping_item_checked(p_item_id uuid, p_checked boolean)
returns void language plpgsql security definer set search_path = public as $$
declare
  v_item public.shopping_items;
begin
  select * into v_item from public.shopping_items where id = p_item_id;
  if v_item.id is null then return; end if;

  if not public.is_family_member(v_item.family_id) then
    raise exception 'Not a member of this family';
  end if;

  update public.shopping_items
  set
    is_checked = p_checked,
    checked_by = case when p_checked then auth.uid() else null end,
    checked_at = case when p_checked then now() else null end
  where id = p_item_id;
end;
$$;

-- ============================================================
-- REALTIME
-- ============================================================
do $$ begin
  alter publication supabase_realtime add table public.tasks;
exception when others then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.shopping_items;
exception when others then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.family_members;
exception when others then null; end $$;
