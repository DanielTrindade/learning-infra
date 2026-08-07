---
id: aws/11-rds-postgres
titulo: RDS no plano de controle, PostgreSQL no data plane
dificuldade: assistido
ministack: true
infraestruturaRealAws: true
---
# RDS no plano de controle, PostgreSQL no data plane

Este Cenário monta o socket do Docker no MiniStack. O container pode controlar o daemon
local; execute apenas os arquivos versionados deste laboratório.

## Crie uma instância

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint rds create-db-instance `
  --db-instance-identifier learning-infra-postgres `
  --db-instance-class db.t3.micro `
  --engine postgres --engine-version 15 `
  --master-username admin --master-user-password lab-seguro-123 `
  --db-name app --allocated-storage 20 `
  --no-publicly-accessible

aws --endpoint-url $endpoint rds wait db-instance-available `
  --db-instance-identifier learning-infra-postgres
```

O plano de controle é emulado, mas o data plane é um PostgreSQL real em container. Ache
o sidecar por label, não por “primeiro Postgres da máquina”:

```powershell
$db = docker ps --filter "label=ministack=rds" --filter "label=db_id=learning-infra-postgres" -q
docker cp esquema.sql "${db}:/tmp/esquema.sql"
docker exec $db psql -U admin -d app -f /tmp/esquema.sql
docker exec $db psql -U admin -d app -tAc "select valor from configuracao where chave='ambiente'"
```

## Limite

DB subnet group e security groups são metadata; eles não filtram pacotes. Snapshot não
é dump real. O SQL prova PostgreSQL local, não Multi-AZ, backup nem failover da AWS.
