-- ============================================================
-- Family OS v0.7 Migration — Notifications (колокольчик)
-- Выполнить в Supabase Dashboard → SQL Editor
-- ============================================================
-- Логика:
--   • Новый товар в списке покупок → уведомление всем членам семьи (кроме автора).
--   • Новая задача без исполнителя  → уведомление всем (кроме автора).
--   • Задача назначена конкретному человеку (при создании или переназначении)
--     → уведомление ТОЛЬКО этому человеку.
--   • Задача выполнена               → уведомление всем (кроме того, кто выполнил).
-- ============================================================

-- 1. Таблица уведомлений (одна строка = одно уведомление одному получателю)
create table if not exists public.notifications (
  id          uuid        primary key default gen_random_uuid(),
  family_id   uuid        not null references public.families(id) on delete cascade,
  user_id     uuid        not null references auth.users(id) on delete cascade,  -- получатель
  actor_id    uuid        references auth.users(id) on delete set null,          -- кто инициировал
  type        text        not null,   -- shopping_added | task_assigned | task_created | task_completed
  title       text        not null,
  body        text,
  entity_type text,                    -- shopping_item | task
  entity_id   uuid,
  is_read     boolean     not null default false,
  created_at  timestamptz not null default now()
);

create index if not exists notifications_user_idx
  on public.notifications (user_id, is_read, created_at desc);

-- 2. RLS: каждый видит и меняет только свои уведомления
alter table public.notifications enable row level security;

drop policy if exists "Users read own notifications" on public.notifications;
create policy "Users read own notifications"
  on public.notifications for select using (user_id = auth.uid());

drop policy if exists "Users update own notifications" on public.notifications;
create policy "Users update own notifications"
  on public.notifications for update using (user_id = auth.uid());

drop policy if exists "Users delete own notifications" on public.notifications;
create policy "Users delete own notifications"
  on public.notifications for delete using (user_id = auth.uid());

-- 3. Имя инициатора (никнейм в семье → имя профиля → «Кто-то»)
create or replace function public.fam_display_name(p_family_id uuid, p_user_id uuid)
returns text language sql security definer stable set search_path = public as $$
  select coalesce(
    (select nickname  from public.family_members where family_id = p_family_id and user_id = p_user_id),
    (select full_name from public.profiles       where id = p_user_id),
    'Кто-то'
  );
$$;

-- 4. Рассылка всем членам семьи (опционально без инициатора)
create or replace function public.notify_family(
  p_family_id uuid, p_actor uuid, p_type text, p_title text, p_body text,
  p_entity_type text, p_entity_id uuid, p_exclude_actor boolean default true
) returns void language plpgsql security definer set search_path = public as $$
begin
  insert into public.notifications
    (family_id, user_id, actor_id, type, title, body, entity_type, entity_id)
  select p_family_id, fm.user_id, p_actor, p_type, p_title, p_body, p_entity_type, p_entity_id
  from public.family_members fm
  where fm.family_id = p_family_id
    and (not p_exclude_actor or fm.user_id <> p_actor);
end;
$$;

-- 5. Триггер: новый товар в списке → всем, кроме автора
create or replace function public.trg_notify_shopping_added()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_actor text;
begin
  v_actor := public.fam_display_name(NEW.family_id, NEW.created_by);
  perform public.notify_family(
    NEW.family_id, NEW.created_by, 'shopping_added',
    'Новый товар в списке',
    v_actor || ' добавил(а): ' || NEW.title,
    'shopping_item', NEW.id, true
  );
  return NEW;
end;
$$;

drop trigger if exists notify_shopping_added on public.shopping_items;
create trigger notify_shopping_added
  after insert on public.shopping_items
  for each row execute function public.trg_notify_shopping_added();

-- 6. Триггер: создание задачи
create or replace function public.trg_notify_task_insert()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_actor text;
begin
  v_actor := public.fam_display_name(NEW.family_id, NEW.created_by);
  if NEW.assigned_to is not null and NEW.assigned_to <> NEW.created_by then
    -- Назначена конкретному человеку → только ему
    insert into public.notifications
      (family_id, user_id, actor_id, type, title, body, entity_type, entity_id)
    values
      (NEW.family_id, NEW.assigned_to, NEW.created_by, 'task_assigned',
       'Вам назначена задача', v_actor || ' поручил(а) вам: ' || NEW.title, 'task', NEW.id);
  elsif NEW.assigned_to is null then
    -- Без исполнителя → всем, кроме автора
    perform public.notify_family(
      NEW.family_id, NEW.created_by, 'task_created',
      'Новая задача', v_actor || ' создал(а) задачу: ' || NEW.title, 'task', NEW.id, true);
  end if;
  return NEW;
end;
$$;

drop trigger if exists notify_task_insert on public.tasks;
create trigger notify_task_insert
  after insert on public.tasks
  for each row execute function public.trg_notify_task_insert();

-- 7. Триггер: переназначение задачи → новому исполнителю
create or replace function public.trg_notify_task_assign()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_actor_id uuid; v_actor text;
begin
  if NEW.assigned_to is distinct from OLD.assigned_to and NEW.assigned_to is not null then
    v_actor_id := coalesce(auth.uid(), NEW.created_by);
    if NEW.assigned_to <> v_actor_id then
      v_actor := public.fam_display_name(NEW.family_id, v_actor_id);
      insert into public.notifications
        (family_id, user_id, actor_id, type, title, body, entity_type, entity_id)
      values
        (NEW.family_id, NEW.assigned_to, v_actor_id, 'task_assigned',
         'Вам назначена задача', v_actor || ' поручил(а) вам: ' || NEW.title, 'task', NEW.id);
    end if;
  end if;
  return NEW;
end;
$$;

drop trigger if exists notify_task_assign on public.tasks;
create trigger notify_task_assign
  after update of assigned_to on public.tasks
  for each row execute function public.trg_notify_task_assign();

-- 8. Триггер: задача выполнена → всем, кроме исполнившего
create or replace function public.trg_notify_task_completed()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_actor_id uuid; v_actor text;
begin
  if NEW.is_completed and not coalesce(OLD.is_completed, false) then
    v_actor_id := coalesce(NEW.completed_by, auth.uid());
    v_actor := public.fam_display_name(NEW.family_id, v_actor_id);
    perform public.notify_family(
      NEW.family_id, v_actor_id, 'task_completed',
      'Задача выполнена', v_actor || ' выполнил(а): ' || NEW.title, 'task', NEW.id, true);
  end if;
  return NEW;
end;
$$;

drop trigger if exists notify_task_completed on public.tasks;
create trigger notify_task_completed
  after update of is_completed on public.tasks
  for each row execute function public.trg_notify_task_completed();

-- 9. Realtime: чтобы колокольчик обновлялся мгновенно
alter publication supabase_realtime add table public.notifications;
