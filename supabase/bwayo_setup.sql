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

-- Also promote the existing account if it was created before this setup script ran.
update public.profiles p
set role = 'admin'
from auth.users u
where p.id = u.id and lower(u.email) = 'nabonyajenipher@gmail.com';
alter table public.profiles enable row level security;
alter table public.posts enable row level security;
grant usage on schema public to authenticated;
grant select on public.profiles to authenticated;
grant select,insert,delete on public.posts to authenticated;
create or replace function public.bwayo_is_admin()
returns boolean language sql stable security definer set search_path = ''
as $ select exists(select 1 from public.profiles where id = auth.uid() and role = 'admin'); $;
drop policy if exists "Read own profile or admin" on public.profiles;
create policy "Read own profile or admin" on public.profiles for select to authenticated
using (id = auth.uid() or public.bwayo_is_admin());
drop policy if exists "Authenticated users read posts" on public.posts;
create policy "Authenticated users read posts" on public.posts for select to authenticated using(true);
drop policy if exists "Users create own posts" on public.posts;
create policy "Users create own posts" on public.posts for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "Users delete own posts or admin deletes any" on public.posts;
create policy "Users delete own posts or admin deletes any" on public.posts for delete to authenticated
using(user_id=auth.uid() or public.bwayo_is_admin());


-- Secure admin-only role management for the app dashboard.
create or replace function public.bwayo_set_user_role(target_user uuid, new_role text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
 if not public.bwayo_is_admin() then
  raise exception 'Administrator access required';
 end if;
 if new_role not in ('user','admin') then
  raise exception 'Invalid role';
 end if;
 if target_user = auth.uid() then
  raise exception 'You cannot change your own administrator role';
 end if;
 update public.profiles set role = new_role where id = target_user;
 if not found then
  raise exception 'User profile not found';
 end if;
end;
$$;
revoke all on function public.bwayo_set_user_role(uuid,text) from public;
grant execute on function public.bwayo_set_user_role(uuid,text) to authenticated;


-- Video uploads for the Bwayo Super App.
alter table public.posts add column if not exists media_url text;
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('bwayo-videos','bwayo-videos',true,104857600,array['video/mp4','video/quicktime','video/webm'])
on conflict (id) do update set public=true,file_size_limit=104857600,allowed_mime_types=array['video/mp4','video/quicktime','video/webm'];

drop policy if exists "Bwayo users upload own videos" on storage.objects;
create policy "Bwayo users upload own videos" on storage.objects
for insert to authenticated
with check (bucket_id='bwayo-videos' and (storage.foldername(name))[1]=auth.uid()::text);

drop policy if exists "Public can view Bwayo videos" on storage.objects;
create policy "Public can view Bwayo videos" on storage.objects
for select to public
using (bucket_id='bwayo-videos');

drop policy if exists "Bwayo users delete own videos" on storage.objects;
create policy "Bwayo users delete own videos" on storage.objects
for delete to authenticated
using (bucket_id='bwayo-videos' and (storage.foldername(name))[1]=auth.uid()::text);


-- Likes and comments for community posts.
create table if not exists public.likes (
 id uuid primary key default gen_random_uuid(),
 post_id uuid not null references public.posts(id) on delete cascade,
 user_id uuid not null references public.profiles(id) on delete cascade,
 created_at timestamptz not null default now(),
 unique(post_id,user_id)
);
create index if not exists likes_post_id_idx on public.likes(post_id);
alter table public.likes enable row level security;
grant select,insert,delete on public.likes to authenticated;
drop policy if exists "Authenticated users read likes" on public.likes;
create policy "Authenticated users read likes" on public.likes
 for select to authenticated using(true);
drop policy if exists "Users add own likes" on public.likes;
create policy "Users add own likes" on public.likes
 for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "Users remove own likes" on public.likes;
create policy "Users remove own likes" on public.likes
 for delete to authenticated using(user_id=auth.uid());

create table if not exists public.comments (
 id uuid primary key default gen_random_uuid(),
 post_id uuid not null references public.posts(id) on delete cascade,
 user_id uuid not null references public.profiles(id) on delete cascade,
 content text not null check(char_length(content) between 1 and 1000),
 created_at timestamptz not null default now()
);
create index if not exists comments_post_id_created_at_idx on public.comments(post_id,created_at);
alter table public.comments enable row level security;
grant select,insert on public.comments to authenticated;
drop policy if exists "Authenticated users read comments" on public.comments;
create policy "Authenticated users read comments" on public.comments
 for select to authenticated using(true);
drop policy if exists "Users add own comments" on public.comments;
create policy "Users add own comments" on public.comments
 for insert to authenticated with check(user_id=auth.uid());
