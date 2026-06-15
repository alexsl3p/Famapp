-- ============================================================
-- Family OS v0.5 Migration — Task priority, comments, photo storage, profile
-- Выполнить в Supabase Dashboard → SQL Editor
-- ============================================================

-- 1. Task priority flag
alter table public.tasks
  add column if not exists is_priority boolean not null default false;

-- 2. Extend create_task RPC with p_is_priority (named params — порядок не важен)
create or replace function public.create_task(
  p_family_id   uuid,
  p_title       text,
  p_description text    default null,
  p_assigned_to uuid    default null,
  p_due_date    date    default null,
  p_repeat_type text    default 'none',
  p_created_by  uuid    default null,
  p_is_priority boolean default false
) returns uuid language plpgsql security definer set search_path = public as $$
declare
  v_user_id uuid := coalesce(p_created_by, auth.uid());
  v_task_id uuid;
begin
  if not public.is_family_member(p_family_id) then
    raise exception 'Not a member of this family';
  end if;

  insert into public.tasks (
    family_id, title, description, assigned_to, created_by, due_date, repeat_type, is_priority
  ) values (
    p_family_id, p_title, p_description, p_assigned_to, v_user_id, p_due_date, p_repeat_type, p_is_priority
  )
  returning id into v_task_id;

  return v_task_id;
end;
$$;

-- 3. Task comments (с возможностью прикрепить фото)
create table if not exists public.task_comments (
  id         uuid primary key default gen_random_uuid(),
  task_id    uuid not null references public.tasks(id) on delete cascade,
  family_id  uuid not null references public.families(id) on delete cascade,
  author_id  uuid references public.profiles(id) on delete set null,
  body       text,
  image_url  text,
  created_at timestamptz not null default now()
);
create index if not exists idx_task_comments_task_id on public.task_comments(task_id);

alter table public.task_comments enable row level security;

drop policy if exists "members read task_comments"   on public.task_comments;
drop policy if exists "members create task_comments" on public.task_comments;
drop policy if exists "members delete task_comments" on public.task_comments;

create policy "members read task_comments"
  on public.task_comments for select using (public.is_family_member(family_id));
create policy "members create task_comments"
  on public.task_comments for insert with check (public.is_family_member(family_id) and author_id = auth.uid());
create policy "members delete task_comments"
  on public.task_comments for delete using (public.is_family_member(family_id));

-- Realtime (если уже добавлено — пропустите эту строку при повторном запуске)
alter publication supabase_realtime add table public.task_comments;

-- 4. Storage buckets для фото (публичное чтение)
insert into storage.buckets (id, name, public) values ('avatars', 'avatars', true)
  on conflict (id) do nothing;
insert into storage.buckets (id, name, public) values ('task-photos', 'task-photos', true)
  on conflict (id) do nothing;

-- Политики storage.objects: публичное чтение, запись/изменение — только авторизованным.
-- (MVP-уровень: любой участник семьи может загрузить в общий бакет.)
drop policy if exists "public read avatars"       on storage.objects;
drop policy if exists "auth write avatars"         on storage.objects;
drop policy if exists "auth update avatars"        on storage.objects;
drop policy if exists "public read task photos"    on storage.objects;
drop policy if exists "auth write task photos"     on storage.objects;

create policy "public read avatars"
  on storage.objects for select using (bucket_id = 'avatars');
create policy "auth write avatars"
  on storage.objects for insert to authenticated with check (bucket_id = 'avatars');
create policy "auth update avatars"
  on storage.objects for update to authenticated using (bucket_id = 'avatars');

create policy "public read task photos"
  on storage.objects for select using (bucket_id = 'task-photos');
create policy "auth write task photos"
  on storage.objects for insert to authenticated with check (bucket_id = 'task-photos');
