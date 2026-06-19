-- ============================================================
-- Family OS v0.8 — Reset statistics (обнуление статистики покупок)
-- Выполнить в Supabase Dashboard → SQL Editor
-- ============================================================
-- Удаляет историю событий инвентаря (на ней строятся представления
-- v_frequent_products_90d и v_purchase_cadence). Текущие остатки в
-- inventory_items не трогаются. Доступно только членам семьи.
-- ============================================================

create or replace function public.reset_family_stats(p_family_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_family_member(p_family_id) then
    raise exception 'Forbidden';
  end if;
  delete from public.inventory_events where family_id = p_family_id;
end;
$$;

grant execute on function public.reset_family_stats(uuid) to authenticated;
