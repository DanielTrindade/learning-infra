# O estado em que a Mirante acordou na segunda-feira. Nada aqui e Terraform.

Write-Output "== estoque =="
if (-not (docker volume ls --format "{{.Name}}" | Select-String -Quiet "^mirante-estoque$")) {
    docker volume create mirante-estoque | Out-Null
}
docker run --rm -v mirante-estoque:/data alpine sh -c "grep -q pedido-4711 /data/estoque.txt 2>/dev/null || echo pedido-4711 > /data/estoque.txt"
Write-Output "volume de estoque no ar, com o pedido-4711 dentro."

Write-Output "== cluster =="
$namespaceExiste = $true
kubectl --context docker-desktop get namespace learning-infra-iac-18 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { $namespaceExiste = $false }
if (-not $namespaceExiste) {
    kubectl --context docker-desktop create namespace learning-infra-iac-18 | Out-Null
}
$manifesto = Join-Path $PSScriptRoot "deployment-mirante-web.yaml"
@"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mirante-web
  namespace: learning-infra-iac-18
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mirante-web
  template:
    metadata:
      labels:
        app: mirante-web
    spec:
      containers:
        - name: web
          image: nginx:1.27-alpine
          ports:
            - containerPort: 80
"@ | Out-File -FilePath $manifesto -Encoding ascii
kubectl --context docker-desktop apply -f $manifesto | Out-Null
kubectl --context docker-desktop -n learning-infra-iac-18 scale deployment mirante-web --replicas=5 | Out-Null
Write-Output "deployment mirante-web no ar, com replicas que o codigo nao declara."

Write-Output "== fila =="
$env:AWS_ACCESS_KEY_ID = "000000000000"
$env:AWS_SECRET_ACCESS_KEY = "test"
$filas = aws --endpoint-url http://127.0.0.1:4566 --region us-east-1 sqs list-queues --query "QueueUrls" --output text 2>$null
if (-not $filas) {
    aws --endpoint-url http://127.0.0.1:4566 --region us-east-1 sqs create-queue --queue-name mirante-pedidos | Out-Null
}
Write-Output "fila mirante-pedidos no ar."

Write-Output ""
Write-Output "ambiente no ar. terraform init, e o plano conta o resto."
