FROM ubuntu:26.04@sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f

ENV LANG=C.UTF-8

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        systemd procps iproute2 sudo curl python3 \
    && systemctl mask tmp.mount \
    && rm -rf /var/lib/apt/lists/*

CMD ["/usr/lib/systemd/systemd"]
