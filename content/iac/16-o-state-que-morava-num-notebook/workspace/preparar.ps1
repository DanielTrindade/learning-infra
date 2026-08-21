# O bucket que guarda o state não pode ser criado pelo Terraform que o usa.
$e = "http://127.0.0.1:4566"
aws --endpoint-url $e --region us-east-1 --no-sign-request s3api create-bucket --bucket mirante-tfstate | Out-Null
aws --endpoint-url $e --region us-east-1 --no-sign-request s3api put-bucket-versioning --bucket mirante-tfstate --versioning-configuration Status=Enabled
Write-Output "bucket de state pronto: mirante-tfstate"
