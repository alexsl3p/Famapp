-- ============================================================
-- Family OS v0.3 Migration — Inventory locations & items
-- Выполнить в Supabase Dashboard → SQL Editor
-- ВАЖНО: сначала применить supabase_v02_migration.sql
-- ============================================================

-- 1. Inventory locations (места хранения)
create table if not exists public.inventory_locations (
  id         uuid primary key default gen_random_uuid(),
  family_id  uuid not null references public.families(id) on delete cascade,
  name       text not null,
  icon       text,
  created_at timestamptz not null default now()
);

create index if not exists idx_inventory_locations_family_id on public.inventory_locations(family_id);

-- 2. Inventory items (текущие остатки)
create table if not exists public.inventory_items (
  id               uuid primary key default gen_random_uuid(),
  family_id        uuid not null references public.families(id) on delete cascade,
  product_id       uuid not null references public.products(id) on delete cascade,
  location_id      uuid references public.inventory_locations(id) on delete set null,
  quantity         numeric(10,2) not null default 0,
  min_quantity     numeric(10,2),
  target_quantity  numeric(10,2),
  expires_at       date,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);

create index if not exists idx_inventory_items_family_id on public.inventory_items(family_id);
create index if not exists idx_inventory_items_product_id on public.inventory_items(product_id);

-- 3. set_updated_at trigger
create trigger set_updated_at before update on public.inventory_items
  for each row execute procedure public.set_updated_at();

-- 4. RLS
alter table public.inventory_locations enable row level security;
alter table public.inventory_items enable row level security;

create policy "members read inventory_locations"
  on public.inventory_locations for select using (public.is_family_member(family_id));
create policy "members create inventory_locations"
  on public.inventory_locations for insert with check (public.is_family_member(family_id));
create policy "members update inventory_locations"
  on public.inventory_locations for update using (public.is_family_member(family_id));
create policy "members delete inventory_locations"
  on public.inventory_locations for delete using (public.is_family_member(family_id));

create policy "members read inventory_items"
  on public.inventory_items for select using (public.is_family_member(family_id));
create policy "members create inventory_items"
  on public.inventory_items for insert with check (public.is_family_member(family_id));
create policy "members update inventory_items"
  on public.inventory_items for update using (public.is_family_member(family_id));
create policy "members delete inventory_items"
  on public.inventory_items for delete using (public.is_family_member(family_id));

-- 5. Realtime
alter publication supabase_realtime add table public.inventory_locations;
alter publication supabase_realtime add table public.inventory_items;

-- 6. RPC: upsert_inventory_item
-- Используется после покупки для обновления остатков
create or replace function public.upsert_inventory_item(
  p_family_id    uuid,
  p_product_id   uuid,
  p_location_id  uuid    default null,
  p_quantity_add numeric  default 1
)
returns public.inventory_items language plpgsql security definer set search_path = public as $$
declare
  existing public.inventory_items%rowtype;
  result   public.inventory_items;
begin
  if not public.is_family_member(p_family_id) then raise exception 'Forbidden'; end if;

  select * into existing
  from public.inventory_items
  where family_id = p_family_id and product_id = p_product_id
  limit 1;

  if existing.id is null then
    insert into public.inventory_items (family_id, product_id, location_id, quantity)
    values (p_family_id, p_product_id, p_location_id, p_quantity_add)
    returning * into result;
  else
    update public.inventory_items
    set quantity = quantity + p_quantity_add, updated_at = now()
    where id = existing.id
    returning * into result;
  end if;

  return result;
end;
$$;
