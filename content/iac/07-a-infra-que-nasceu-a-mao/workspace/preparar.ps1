# Sobe a infraestrutura da Mirante do jeito que ela nasceu: à mão, sem código.
docker volume create mirante-arquivos | Out-Null
docker network create mirante-interna | Out-Null
docker run -d --name mirante-relatorios --restart unless-stopped --network mirante-interna nginx:1.27-alpine | Out-Null
Write-Output "infraestrutura no ar. Nenhuma linha de Terraform foi escrita."
docker ps --filter name=mirante-relatorios --format '{{.Names}}  {{.Status}}'
