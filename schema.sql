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
  description text,
  icon text,
  status text default 'active',
  created_at timestamptz default now()
);

create table if not exists products (
  id uuid primary key default gen_random_uuid(),
  supplier_id uuid references suppliers(id) on delete cascade,
  category_id uuid references categories(id),
  product_name_ar text not null,
  unit text,
  available_qty numeric default 0,
  min_wholesale_qty numeric default 0,
  wholesale_price numeric default 0,
  min_super_wholesale_qty numeric default 0,
  super_wholesale_price numeric default 0,
  governorate text,
  status text default 'pending',
  created_at timestamptz default now()
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
alter table contact_messages enable row level security;

create policy "public active categories" on categories for select using (status='active');
create policy "public published products" on products for select using (status='published');
create policy "anyone contact" on contact_messages for insert with check (true);
