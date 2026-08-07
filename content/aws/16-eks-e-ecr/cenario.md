---
id: aws/16-eks-e-ecr
titulo: Um EKS local que é k3s — e isso importa
dificuldade: mestre
ministack: true
infraestruturaRealAws: true
---
# Um EKS local que é k3s — e isso importa

O MiniStack cria um cluster k3s real em sidecar e expõe uma API compatível com o plano de
controle EKS. Este Cenário monta o socket do Docker e inicia um container privilegiado.

## Objetivo

1. Crie `learning-infra-eks` pela API EKS e aguarde `ACTIVE`.
2. Grave o kubeconfig administrativo do k3s em `./kubeconfig-eks`, nunca no kubeconfig
   global.
3. Crie o repositório ECR `learning-infra-eks-app` e publique a imagem 1.0.
4. Ajuste `deployment.yaml` para a URI do registry local.
5. Aplique no cluster e obtenha duas réplicas disponíveis.

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint eks create-cluster `
  --name learning-infra-eks `
  --role-arn arn:aws:iam::000000000000:role/learning-infra-eks `
  --resources-vpc-config subnetIds=subnet-local
aws --endpoint-url $endpoint eks wait cluster-active --name learning-infra-eks

$sidecar = "ministack-eks-us-east-1-learning-infra-eks"
docker cp "${sidecar}:/etc/rancher/k3s/k3s.yaml" ./kubeconfig-eks
kubectl --kubeconfig ./kubeconfig-eks config set-cluster default `
  --server=https://127.0.0.1:16443
kubectl --kubeconfig ./kubeconfig-eks get nodes
```

`eks update-kubeconfig` reproduz o contrato da AWS e gera autenticação por
`eks get-token`; na versão local testada esse token não autentica no k3s. Copiar o
kubeconfig administrativo é uma adaptação explícita do emulador — e uma credencial
poderosa que deve ser tratada como segredo, mesmo sendo descartável.

Use na imagem do Deployment a URI retornada por `create-repository`, no formato
`000000000000.dkr.ecr.us-east-1.amazonaws.com/learning-infra-eks-app:1.0`. O MiniStack
configura o mirror desse hostname dentro do k3s. Não substitua silenciosamente a imagem
por uma pública: isso apagaria o problema que o Cenário quer ensinar.

## Limite

O workload Kubernetes é real, mas o control plane é k3s, não EKS gerenciado. Addons,
IAM, VPC CNI, Fargate e integração com a rede AWS continuam sem prova local.
