-- ============================================================
-- Family OS v0.4 Migration — Statistics views
-- Выполнить в Supabase Dashboard → SQL Editor
-- ВАЖНО: сначала применить v02 и v03 миграции
-- ВАЖНО: security_invoker = true — иначе RLS обходится!
-- ============================================================

-- 1. Часто покупаемые товары (последние 90 дней)
create or replace view public.v_frequent_products_90d
  with (security_invoker = true) as
  select
    ie.product_id,
    ie.family_id,
    p.name as product_name,
    p.brand,
    p.image_url,
    count(*) as purchase_count,
    max(ie.event_at) as last_purchased_at
  from public.inventory_events ie
  join public.products p on p.id = ie.product_id
  where ie.event_type = 'purchase'
    and ie.event_at > now() - interval '90 days'
  group by ie.product_id, ie.family_id, p.name, p.brand, p.image_url;

-- 2. Статус инвентаря (с join на продукты)
create or replace view public.v_inventory_status
  with (security_invoker = true) as
  select
    ii.id,
    ii.family_id,
    ii.product_id,
    ii.location_id,
    ii.quantity,
    ii.min_quantity,
    ii.target_quantity,
    ii.expires_at,
    ii.updated_at,
    p.name as product_name,
    p.brand,
    p.image_url,
    p.product_type,
    case
      when ii.quantity <= 0 then 'out'
      when ii.min_quantity is not null and ii.quantity <= ii.min_quantity then 'low'
      when ii.expires_at is not null and ii.expires_at <= current_date + interval '3 days' then 'expiring'
      else 'ok'
    end as status
  from public.inventory_items ii
  join public.products p on p.id = ii.product_id;

-- 3. Кадência покупок (среднее кол-во дней между покупками)
create or replace view public.v_purchase_cadence
  with (security_invoker = true) as
  select
    ie.product_id,
    ie.family_id,
    p.name as product_name,
    count(*) as total_purchases,
    avg(
      extract(epoch from (
        lead(ie.event_at) over (
          partition by ie.product_id, ie.family_id
          order by ie.event_at
        ) - ie.event_at
      )) / 86400
    ) as avg_days_between_purchases,
    max(ie.event_at) as last_purchased_at
  from public.inventory_events ie
  join public.products p on p.id = ie.product_id
  where ie.event_type = 'purchase'
  group by ie.product_id, ie.family_id, p.name;
