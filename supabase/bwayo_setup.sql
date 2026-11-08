-- Run this entire script once in Supabase SQL Editor.
-- Only this email is automatically assigned the administrator role.
create table if not exists public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 name text not null default 'Member',
 role text not null default 'user' check (role in ('user','admin')),
 created_at timestamptz not null default now()
);
create table if not exists public.posts (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references public.profiles(id) on delete cascade,
 content text not null check (char_length(content) between 1 and 2000),
 created_at timestamptz not null default now()
);
create index if not exists posts_created_at_idx on public.posts(created_at desc);
create or replace function public.bwayo_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
 insert into public.profiles(id,name,role) values (
  new.id,
  coalesce(nullif(new.raw_user_meta_data ->> 'name',''),'Member'),
  case when lower(new.email) = 'nabonyajenipher@gmail.com' then 'admin' else 'user' end
 ) on conflict(id) do nothing;
 return new;
end; $$;
drop trigger if exists on_auth_user_created_bwayo on auth.users;
create trigger on_auth_user_created_bwayo after insert on auth.users
for each row execute procedure public.bwayo_new_user();
alter table public.profiles enable row level security;
alter table public.posts enable row level security;
grant usage on schema public to authenticated;
grant select on public.profiles to authenticated;
grant select,insert,delete on public.posts to authenticated;
drop policy if exists "Read own profile or admin" on public.profiles;
create policy "Read own profile or admin" on public.profiles for select to authenticated
using (id=auth.uid() or exists(select 1 from public.profiles me where me.id=auth.uid() and me.role='admin'));
drop policy if exists "Authenticated users read posts" on public.posts;
create policy "Authenticated users read posts" on public.posts for select to authenticated using(true);
drop policy if exists "Users create own posts" on public.posts;
create policy "Users create own posts" on public.posts for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "Users delete own posts or admin deletes any" on public.posts;
create policy "Users delete own posts or admin deletes any" on public.posts for delete to authenticated
using(user_id=auth.uid() or exists(select 1 from public.profiles me where me.id=auth.uid() and me.role='admin'));
