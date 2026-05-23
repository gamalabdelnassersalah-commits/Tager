create extension if not exists "pgcrypto";

create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  phone text,
  whatsapp text,
  user_type text check (user_type in ('supplier','customer','admin')) not null default 'customer',
  status text default 'active',
  created_at timestamptz default now()
);

create table if not exists suppliers (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references profiles(id) on delete cascade,
  company_name text not null,
  responsible_person text,
  commercial_registration text,
  tax_card text,
  vat_number text,
  governorate text,
  delivery_areas text,
  verification_status text default 'pending',
  created_at timestamptz default now()
);

create table if not exists customers (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references profiles(id) on delete cascade,
  business_name text not null,
  responsible_person text,
  business_type text,
  governorate text,
  address text,
  created_at timestamptz default now()
);

create table if not exists categories (
  id uuid primary key default gen_random_uuid(),
  name_ar text not null,
  name_en text,
  description text,
  image_url text,
  icon text,
  status text default 'active',
  created_at timestamptz default now()
);

create table if not exists products (
  id uuid primary key default gen_random_uuid(),
  supplier_id uuid references suppliers(id) on delete cascade,
  category_id uuid references categories(id),
  product_name_ar text not null,
  product_name_en text,
  brand text,
  country_of_origin text,
  unit text,
  description text,
  image_url text,
  icon text,
  available_qty numeric default 0,
  min_wholesale_qty numeric default 0,
  wholesale_price numeric default 0,
  min_super_wholesale_qty numeric default 0,
  super_wholesale_price numeric default 0,
  vat_included boolean default false,
  governorate text,
  delivery_areas text,
  delivery_time text,
  status text default 'pending',
  created_at timestamptz default now()
);

create table if not exists orders (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid references customers(id) on delete cascade,
  supplier_id uuid references suppliers(id) on delete cascade,
  order_number text unique,
  subtotal numeric default 0,
  vat_amount numeric default 0,
  delivery_fees numeric default 0,
  commission_amount numeric default 0,
  total_amount numeric default 0,
  payment_method text,
  payment_status text default 'pending',
  order_status text default 'new',
  created_at timestamptz default now()
);

create table if not exists order_lines (
  id uuid primary key default gen_random_uuid(),
  order_id uuid references orders(id) on delete cascade,
  product_id uuid references products(id),
  quantity numeric not null,
  price_type text check (price_type in ('wholesale','super_wholesale')),
  unit_price numeric not null,
  total_price numeric not null
);

create table if not exists contact_messages (
  id uuid primary key default gen_random_uuid(),
  name text,
  phone text,
  email text,
  user_type text,
  subject text,
  message text,
  status text default 'new',
  created_at timestamptz default now()
);

alter table profiles enable row level security;
alter table suppliers enable row level security;
alter table customers enable row level security;
alter table categories enable row level security;
alter table products enable row level security;
alter table orders enable row level security;
alter table order_lines enable row level security;
alter table contact_messages enable row level security;

drop policy if exists "insert own profile" on profiles;
create policy "insert own profile" on profiles for insert with check (auth.uid() = id);
drop policy if exists "select own profile" on profiles;
create policy "select own profile" on profiles for select using (auth.uid() = id);
drop policy if exists "update own profile" on profiles;
create policy "update own profile" on profiles for update using (auth.uid() = id);

drop policy if exists "public active categories" on categories;
create policy "public active categories" on categories for select using (status='active');

drop policy if exists "public published products" on products;
create policy "public published products" on products for select using (status='published');

drop policy if exists "supplier insert own products" on products;
create policy "supplier insert own products" on products for insert with check (
  supplier_id in (select id from suppliers where user_id = auth.uid())
);

drop policy if exists "supplier update own products" on products;
create policy "supplier update own products" on products for update using (
  supplier_id in (select id from suppliers where user_id = auth.uid())
);

drop policy if exists "insert own supplier" on suppliers;
create policy "insert own supplier" on suppliers for insert with check (user_id = auth.uid());
drop policy if exists "select own supplier" on suppliers;
create policy "select own supplier" on suppliers for select using (user_id = auth.uid());

drop policy if exists "insert own customer" on customers;
create policy "insert own customer" on customers for insert with check (user_id = auth.uid());
drop policy if exists "select own customer" on customers;
create policy "select own customer" on customers for select using (user_id = auth.uid());

drop policy if exists "anyone contact" on contact_messages;
create policy "anyone contact" on contact_messages for insert with check (true);
