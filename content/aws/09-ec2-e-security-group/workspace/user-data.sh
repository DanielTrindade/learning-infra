#!/bin/sh
# Em EC2 real este script instalaria dependências e iniciaria a aplicação.
# No MiniStack ele é preservado como metadata; não é executado.
echo "iniciar-api --porta 8080" >/var/log/learning-infra-bootstrap.log

