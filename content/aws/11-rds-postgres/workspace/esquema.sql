create table if not exists configuracao (
  chave text primary key,
  valor text not null
);

insert into configuracao(chave, valor)
values ('ambiente', 'ministack')
on conflict (chave) do update set valor = excluded.valor;

