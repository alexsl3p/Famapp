-- ============================================================
-- Family OS v0.2 Migration — Products & Inventory foundations
-- Выполнить в Supabase Dashboard → SQL Editor
-- ============================================================

-- 1. Products table (семейный каталог товаров)
create table if not exists public.products (
  id                 uuid primary key default gen_random_uuid(),
  family_id          uuid not null references public.families(id) on delete cascade,
  name               text not null,
  brand              text,
  barcode            text,
  barcode_normalized text,
  package_size       text,
  default_unit       text,
  product_type       text not null default 'other'
    check (product_type in ('food','household_consumable','household_item','other')),
  image_url          text,
  product_url        text,
  notes              text,
  source             text,
  created_by         uuid not null references public.profiles(id) on delete cascade,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);

-- Unique barcode per family
create unique index if not exists unique_product_barcode_per_family
  on public.products(family_id, barcode_normalized)
  where barcode_normalized is not null;

-- Indexes
create index if not exists idx_products_family_id on public.products(family_id);
create index if not exists idx_products_barcode_normalized on public.products(barcode_normalized);

-- 2. Extend shopping_items with product_id
alter table public.shopping_items
  add column if not exists product_id uuid references public.products(id) on delete set null;

-- 3. Inventory events table (история покупок)
create table if not exists public.inventory_events (
  id                uuid primary key default gen_random_uuid(),
  family_id         uuid not null references public.families(id) on delete cascade,
  product_id        uuid not null references public.products(id) on delete cascade,
  event_type        text not null check (event_type in ('purchase','consume','adjust')),
  quantity          numeric(10,2),
  price             numeric(10,2),
  event_at          timestamptz not null default now(),
  created_by        uuid references public.profiles(id) on delete set null
);

create index if not exists idx_inventory_events_family_id on public.inventory_events(family_id);
create index if not exists idx_inventory_events_product_id on public.inventory_events(product_id);

-- 4. RLS on new tables
alter table public.products enable row level security;
alter table public.inventory_events enable row level security;

create policy "members read products"
  on public.products for select using (public.is_family_member(family_id));
create policy "members create products"
  on public.products for insert with check (public.is_family_member(family_id) and created_by = auth.uid());
create policy "members update products"
  on public.products for update using (public.is_family_member(family_id));
create policy "members delete products"
  on public.products for delete using (public.is_family_member(family_id));

create policy "members read inventory events"
  on public.inventory_events for select using (public.is_family_member(family_id));
create policy "members create inventory events"
  on public.inventory_events for insert with check (public.is_family_member(family_id));

-- 5. set_updated_at trigger on products
create trigger set_updated_at before update on public.products
  for each row execute procedure public.handle_updated_at();

-- 6. Extend mark_shopping_item_checked RPC to write inventory_event on purchase
create or replace function public.mark_shopping_item_checked(
  p_item_id uuid,
  p_checked  boolean default true
)
returns void language plpgsql security definer set search_path = public as $$
declare item public.shopping_items%rowtype;
begin
  select * into item from public.shopping_items where id = p_item_id;
  if item.id is null then raise exception 'Item not found'; end if;
  if not public.is_family_member(item.family_id) then raise exception 'Forbidden'; end if;

  if p_checked then
    update public.shopping_items
    set is_checked = true, checked_by = auth.uid(),
        checked_at = now(), updated_at = now()
    where id = p_item_id;

    -- v0.2: write inventory_event if product_id is set
    if item.product_id is not null then
      insert into public.inventory_events
        (family_id, product_id, event_type, created_by)
      values
        (item.family_id, item.product_id, 'purchase', auth.uid());
    end if;
  else
    update public.shopping_items
    set is_checked = false, checked_by = null,
        checked_at = null, updated_at = now()
    where id = p_item_id;
  end if;
end;
$$;

-- 7. Realtime for products and inventory_events
alter publication supabase_realtime add table public.products;
alter publication supabase_realtime add table public.inventory_events;

-- 8. RPC: create_product
create or replace function public.create_product(
  p_family_id    uuid,
  p_name         text,
  p_brand        text        default null,
  p_barcode      text        default null,
  p_package_size text        default null,
  p_default_unit text        default null,
  p_product_type text        default 'other',
  p_image_url    text        default null,
  p_product_url  text        default null,
  p_notes        text        default null,
  p_source       text        default null
)
returns public.products language plpgsql security definer set search_path = public as $$
declare
  new_barcode_norm text;
  result           public.products;
begin
  if not public.is_family_member(p_family_id) then raise exception 'Not a family member'; end if;

  -- normalize barcode
  if p_barcode is not null then
    new_barcode_norm := upper(trim(replace(replace(p_barcode, '-', ''), ' ', '')));
  end if;

  insert into public.products
    (family_id, name, brand, barcode, barcode_normalized, package_size,
     default_unit, product_type, image_url, product_url, notes, source, created_by)
  values
    (p_family_id, p_name, p_brand, p_barcode, new_barcode_norm, p_package_size,
     p_default_unit, p_product_type, p_image_url, p_product_url, p_notes, p_source, auth.uid())
  returning * into result;

  return result;
end;
$$;
