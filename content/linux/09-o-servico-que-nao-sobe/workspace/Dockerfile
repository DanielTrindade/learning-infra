FROM ubuntu:26.04@sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f

ENV LANG=C.UTF-8

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        systemd procps iproute2 sudo curl python3 less nano file lsof \
    && systemctl mask tmp.mount \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /srv/catalogo /etc/aurora /var/log/aurora

RUN printf '%s\n' \
        '<h1>Aurora — catálogo</h1>' \
        '<p>Editora Aurora. Catálogo de títulos.</p>' \
        > /srv/catalogo/index.html \
    && printf '%s\n' \
        '# Catálogo da Aurora — montado em 2019, sem documentação.' \
        'porta=8040' \
        'raiz=/srv/catalogo' \
        > /etc/aurora/catalogo.conf

RUN python3 <<'PY'
import random
random.seed(2026)
clientes = ["livraria-do-porto", "banca-central", "papelaria-sul",
            "leitura-norte", "sebo-da-praca"]
linhas = []
pedido = 4000
for hora in range(24):
    for minuto in range(0, 60, 2):
        for segundo in (7, 23, 51):
            pedido += 1
            cliente = random.choice(clientes)
            if cliente == "livraria-do-porto":
                status = 500 if 2 <= hora <= 4 else random.choices([200, 404], [95, 5])[0]
            else:
                status = random.choices([200, 404, 500], [92, 5, 3])[0]
            ms = random.randint(18, 400)
            linhas.append(
                f"2026-08-14T{hora:02d}:{minuto:02d}:{segundo:02d} "
                f"pedido={pedido} cliente={cliente} status={status} ms={ms}")
with open("/var/log/aurora/pedidos.log", "w") as saida:
    saida.write("\n".join(linhas) + "\n")
PY

CMD ["/usr/lib/systemd/systemd"]
