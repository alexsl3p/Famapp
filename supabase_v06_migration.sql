-- ============================================================
-- Family OS v0.6 Migration — Two-way Shopping ↔ Inventory sync
-- Выполнить в Supabase Dashboard → SQL Editor
-- ============================================================
-- Поведение:
--   • Отметили товар купленным в списке  → товар попадает в инвентарь (+кол-во).
--     Если у позиции не было привязанного продукта — он создаётся по названию.
--   • Сняли галочку                       → количество в инвентаре уменьшается.
--   • Уменьшили количество в инвентаре до 0 → позиция возвращается
--     в список покупок без галочки (готова к повторной покупке).
-- ============================================================

-- 1. mark_shopping_item_checked: при покупке заводим продукт и пополняем инвентарь
create or replace function public.mark_shopping_item_checked(
  p_item_id uuid,
  p_checked  boolean default true
)
returns void language plpgsql security definer set search_path = public as $$
declare
  item         public.shopping_items%rowtype;
  v_product_id uuid;
  v_qty        numeric;
begin
  select * into item from public.shopping_items where id = p_item_id;
  if item.id is null then raise exception 'Item not found'; end if;
  if not public.is_family_member(item.family_id) then raise exception 'Forbidden'; end if;

  -- Достаём число из текстового quantity ("2", "500 г", "1.5 кг"); по умолчанию 1.
  v_qty := nullif(regexp_replace(coalesce(item.quantity, ''), '[^0-9.].*$', ''), '')::numeric;
  if v_qty is null or v_qty <= 0 then v_qty := 1; end if;

  if p_checked then
    -- Гарантируем наличие продукта (для инвентаря).
    v_product_id := item.product_id;
    if v_product_id is null then
      select id into v_product_id
      from public.products
      where family_id = item.family_id and lower(name) = lower(item.title)
      limit 1;
      if v_product_id is null then
        insert into public.products (family_id, name, product_type, source, created_by)
        values (item.family_id, item.title, 'other', 'shopping', auth.uid())
        returning id into v_product_id;
      end if;
    end if;

    update public.shopping_items
    set is_checked = true, checked_by = auth.uid(),
        checked_at = now(), updated_at = now(), product_id = v_product_id
    where id = p_item_id;

    insert into public.inventory_events
      (family_id, product_id, event_type, created_by)
    values
      (item.family_id, v_product_id, 'purchase', auth.uid());

    perform public.upsert_inventory_item(item.family_id, v_product_id, null, v_qty);
  else
    update public.shopping_items
    set is_checked = false, checked_by = null,
        checked_at = null, updated_at = now()
    where id = p_item_id;

    -- Откат покупки — уменьшаем остаток в инвентаре (не ниже 0).
    if item.product_id is not null then
      update public.inventory_items
      set quantity = greatest(quantity - v_qty, 0), updated_at = now()
      where family_id = item.family_id and product_id = item.product_id;
    end if;
  end if;
end;
$$;

-- 2. set_inventory_quantity: задаём количество; при 0 возвращаем товар в список покупок
create or replace function public.set_inventory_quantity(
  p_item_id  uuid,
  p_quantity numeric
)
returns void language plpgsql security definer set search_path = public as $$
declare
  inv        public.inventory_items%rowtype;
  v_list_id  uuid;
  v_name     text;
  v_existing uuid;
begin
  select * into inv from public.inventory_items where id = p_item_id;
  if inv.id is null then raise exception 'Item not found'; end if;
  if not public.is_family_member(inv.family_id) then raise exception 'Forbidden'; end if;

  if p_quantity > 0 then
    update public.inventory_items
    set quantity = p_quantity, updated_at = now()
    where id = p_item_id;
    return;
  end if;

  -- Количество дошло до 0 → возвращаем товар в список покупок без галочки.
  select name into v_name from public.products where id = inv.product_id;

  select id into v_list_id from public.shopping_lists
  where family_id = inv.family_id
  order by created_at asc
  limit 1;

  if v_list_id is not null then
    select id into v_existing from public.shopping_items
    where list_id = v_list_id and product_id = inv.product_id and is_checked = false
    limit 1;

    if v_existing is null then
      insert into public.shopping_items
        (family_id, list_id, title, product_id, created_by, is_checked)
      values
        (inv.family_id, v_list_id, coalesce(v_name, 'Товар'), inv.product_id, auth.uid(), false);
    end if;
  end if;

  update public.inventory_items
  set quantity = 0, updated_at = now()
  where id = p_item_id;
end;
$$;
