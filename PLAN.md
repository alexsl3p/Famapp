# Kinly / Family OS — актуальный план

## Контекст

**Стек:** Kotlin + Jetpack Compose (Android) · Supabase (supabase-kt 3.0.0) · Hilt · Realtime
**Supabase проект:** `jpgezwiprogeciqxptxq` (уже создан, миграция применена)
**Дизайн:** тёмный glassmorphism — НЕ менять (оставить как есть)
**Бранч разработки:** `claude/android-app-design-h2u1af`

---

## Что УЖЕ сделано ✅

### Инфраструктура
- `FamApplication.kt` + `SupabaseModule.kt` (Hilt + Supabase client)
- `local.properties`: supabase_url, supabase_anon_key, google_web_client_id
- `app/build.gradle.kts`: все зависимости (Hilt, supabase-kt 3.0.0, ktor 3.0.0, play-services-auth)
- `libs.versions.toml`: полный набор версий
- `lib/DataStoreModule.kt`: Hilt-модуль для DataStore<Preferences>

### Auth (v0.1) ✅
- `GoogleSignInHelper` (legacy `GoogleSignInClient`) → Activity Result API → не крашится
- `AuthRepository` → `signInWithGoogle(idToken, nonce)` → Supabase IDToken flow
- `AuthViewModel`: Loading / Unauthenticated / NeedsFamily / Authenticated
- `WelcomeScreen`: кнопка Google + "Продолжить без входа (тест)"
- `OnboardingFamilyScreen`: create/join режимы, ввод 8-значного кода

### Family (v0.1) ✅
- `FamilyRepository`: createFamily (RPC), joinFamily, getFamily, getMembers (с join profiles), regenerateInviteCode, leaveFamily
- `FamilyViewModel`: load, createFamily, joinFamily, regenerateCode
- `FamilyScreen`: invite code + copy + regenerate, список участников с именами

### Tasks (v0.1) ✅
- `TasksRepository`: getTasks, createTask (RPC), completeTask (RPC), uncompleteTask, deleteTask
- `TasksViewModel`: Realtime подписка на `tasks`, фильтры All/Mine/Done, snackbar на roll-forward
- `TasksScreen`: фильтр-табы, FAB с полным диалогом (title, description, assignedTo, dueDate, repeatType), чекбоксы, SnackbarHost, attribution

### Shopping (v0.1) ✅
- `ShoppingRepository`: getLists, getItems, addItem, checkItem (RPC), clearChecked, createList
- `ShoppingViewModel`: Realtime подписка на `shopping_items`, оптимистичный апдейт, DataStore офлайн-кеш
- `ShoppingScreen`: список, чекбоксы со strikethrough, FAB, "Очистить купленное"

### Home (v0.1) ✅
- `HomeScreen`: реальные данные из TasksVM + ShoppingVM + FamilyVM

---

## КЛЮЧЕВЫЕ ТЕХНИЧЕСКИЕ ПАТТЕРНЫ

### RPC decode (ВАЖНО!)
Supabase RPC возвращает bare JSON объект, НЕ массив. Используй:
```kotlin
private val rpcJson = Json { ignoreUnknownKeys = true }
// ...
val result = supabase.postgrest.rpc("my_rpc", params)
rpcJson.decodeFromString<MyType>(result.data)
```
НЕ используй `decodeSingle<T>()` — он ожидает массив и крашится.

### PostgREST join
```kotlin
supabase.postgrest["family_members"]
    .select(Columns.raw("*, profiles!user_id(full_name, avatar_url, color)"))
    { filter { eq("family_id", familyId) } }
    .decodeList<FamilyMember>()
```

### Google Sign-In (legacy)
Используем `play-services-auth 21.2.0` — Credential Manager API падает с `TYPE_NO_CREDENTIAL` на устройствах без настроенного аккаунта. Nonce не передаётся (legacy API не поддерживает).

### SessionStatus в supabase-kt 3.x
`Initializing`, `RefreshFailure`, `Authenticated`, `NotAuthenticated` — используй именно их.

---

## SQL миграции (применить вручную в Supabase Dashboard → SQL Editor)

### v0.2 → файл `supabase_v02_migration.sql` (в корне репо)
- Таблица `products` с unique index по barcode_normalized per family
- `alter table shopping_items add column product_id`
- Таблица `inventory_events`
- RLS политики на новые таблицы через `is_family_member(family_id)`
- Расширенный RPC `mark_shopping_item_checked` — пишет inventory_event при покупке с product_id
- RPC `create_product` (SECURITY DEFINER)
- Realtime для products и inventory_events

### v0.3 → файл `supabase_v03_migration.sql` (создать)
```sql
create table public.inventory_locations (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references public.families(id) on delete cascade,
  name text not null,
  created_at timestamptz not null default now()
);

create table public.inventory_items (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references public.families(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete cascade,
  location_id uuid references public.inventory_locations(id) on delete set null,
  quantity numeric(10,2) not null default 0,
  min_quantity numeric(10,2),
  target_quantity numeric(10,2),
  expires_at date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- set_updated_at триггер на inventory_items
-- RLS: is_family_member(family_id) на обеих таблицах
-- Realtime: inventory_locations, inventory_items
```

### v0.4 → файл `supabase_v04_migration.sql` (создать)
```sql
-- ВАЖНО: security_invoker = true — иначе RLS обходится!
create view v_frequent_products_90d
  with (security_invoker = true) as
  select product_id, count(*) as purchase_count, family_id
  from inventory_events
  where event_type = 'purchase' and event_at > now() - interval '90 days'
  group by product_id, family_id;

create view v_inventory_status
  with (security_invoker = true) as
  select ii.*, p.name as product_name, p.image_url,
    case
      when ii.quantity = 0 then 'out'
      when ii.min_quantity is not null and ii.quantity <= ii.min_quantity then 'low'
      when ii.expires_at is not null and ii.expires_at <= current_date + interval '3 days' then 'expiring'
      else 'ok'
    end as status
  from inventory_items ii
  join products p on p.id = ii.product_id;

create view v_purchase_cadence
  with (security_invoker = true) as
  select product_id, family_id,
    avg(extract(epoch from (lead(event_at) over (partition by product_id, family_id order by event_at) - event_at))/86400) as avg_days_between
  from inventory_events
  where event_type = 'purchase'
  group by product_id, family_id;
```

---

## v0.2 — Продукты и история покупок (Android код)

**Статус:** SQL миграция написана (`supabase_v02_migration.sql`), Android код — В ПРОЦЕССЕ

### Файлы для создания:
- `data/models/Product.kt` — `@Serializable` data class
- `features/products/ProductRepository.kt` — getProducts, createProduct, findByBarcode
- `features/products/ProductViewModel.kt` — HiltViewModel
- Обновить `ShoppingScreen` — поиск по products при добавлении, автолинк product_id

---

## v0.3 — Полный инвентарь + сканер

### Android файлы:
- `data/models/InventoryItem.kt`, `InventoryEvent.kt`, `InventoryLocation.kt`
- `features/inventory/InventoryRepository.kt` + `InventoryViewModel.kt`
- `ui/screens/InventoryScreen.kt` — вкладка внутри Shopping (TabRow: Списки / Инвентарь)
- `ui/components/InventoryItemCard.kt` — StatusBadge (Заканчивается / Закончилось / Скоро истекает / OK)
- `ui/screens/ScannerScreen.kt` — CameraX + MLKit barcode-scanning

### Новые зависимости (добавить в build.gradle.kts):
```
camera-camera2, camera-lifecycle, camera-view, mlkit-barcode-scanning
```

### Open Food Facts API:
```
GET https://world.openfoodfacts.org/api/v3/product/{barcode}.json
```

### Нормализация баркода:
```kotlin
fun normalizeBarcode(raw: String) = raw.trim().uppercase().replace("-", "").replace(" ", "")
```

---

## v0.4 — Статистика

### Android файлы:
- `features/stats/StatsRepository.kt` + `StatsViewModel.kt`
- `ui/screens/StatsScreen.kt` — блоки StatCard
- Добавить 5-й пункт в `navigation/Navigation.kt` (`Screen.Stats`)
- Добавить в `MainAppContent` (MainActivity.kt)

---

## Файлы НЕ ТРОГАТЬ
- `ui/theme/` — Color.kt, Theme.kt, Type.kt
- `ui/components/` — MeshBackground, GlassCard, GlassButton, KinlyTopBar
- `lib/SupabaseModule.kt`
- `navigation/Navigation.kt` (расширять, не переписывать)

---

## Безопасность (жёсткие правила)
- `service_role` ключ — никогда в APK
- Все операции с family_members — только через RPC
- SQL views — `security_invoker = true` (без этого RLS обходится)
- `anon_key` — только в клиенте, только через `BuildConfig`

---

## DoD v0.1 (чеклист)
- [x] Google Sign-In работает (legacy play-services-auth)
- [x] Профиль создаётся автоматически (handle_new_user триггер)
- [x] Создать семью → invite_code, список «Продукты», active_family_id
- [x] Вступить по коду → неверный код = ошибка сервера
- [x] Создать задачу через RPC create_task
- [x] Повторяющаяся задача: complete → roll-forward, undo-snackbar 5 сек
- [x] Отметить товар купленным → оптимистичный апдейт + realtime
- [x] «Очистить купленное» работает
- [x] Realtime на tasks и shopping_items
- [x] Имена участников в FamilyScreen (join с profiles)
- [x] Task dialog: due_date, assigned_to, repeat_type
- [x] Attribution на карточках Tasks
- [x] Офлайн кеш shopping (DataStore)
