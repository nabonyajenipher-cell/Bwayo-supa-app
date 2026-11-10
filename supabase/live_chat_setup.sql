-- Run this script once in the Supabase SQL Editor to enable Bwayo Live Community Chat.
create table if not exists public.chat_messages (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references public.profiles(id) on delete cascade,
 content text not null check (char_length(content) between 1 and 1000),
 created_at timestamptz not null default now()
);
create index if not exists chat_messages_created_at_idx
 on public.chat_messages(created_at asc);
alter table public.chat_messages enable row level security;
grant select, insert on public.chat_messages to authenticated;
drop policy if exists "Bwayo members read chat" on public.chat_messages;
create policy "Bwayo members read chat"
 on public.chat_messages for select to authenticated using (true);
drop policy if exists "Members send own chat messages" on public.chat_messages;
create policy "Members send own chat messages"
 on public.chat_messages for insert to authenticated
 with check (user_id = auth.uid());
