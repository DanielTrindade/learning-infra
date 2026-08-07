---
id: kubernetes/14-incidente-final
titulo: O incidente das duas verdades
dificuldade: mestre
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-14
manifestosIniciais: setup
---
# O incidente das duas verdades

A equipe diz que a aplicação está rodando. O cliente diz que não existe backend. As duas
afirmações são verdadeiras sob métricas diferentes.

Recupere o ambiente para que `http://api` responda **sagui** a partir do Pod `cliente`,
com duas réplicas disponíveis. Há mais de um defeito independente.

## O que existe

`setup/stack.yaml` contém ConfigMap, Deployment, Service e cliente de diagnóstico. O
ambiente já foi aplicado. Você pode alterar qualquer parte e reaplicar.

Não há ordem obrigatória, mas existe uma cadeia de evidências:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-14 get deployment,pod,service,endpointslice
kubectl --context docker-desktop -n learning-infra-k8s-14 get pods --show-labels
kubectl --context docker-desktop -n learning-infra-k8s-14 describe deployment api
kubectl --context docker-desktop -n learning-infra-k8s-14 describe pod -l componente=api
kubectl --context docker-desktop -n learning-infra-k8s-14 describe service api
kubectl --context docker-desktop -n learning-infra-k8s-14 logs deployment/api
kubectl --context docker-desktop -n learning-infra-k8s-14 exec deployment/cliente -- wget -S -O- http://api
```

## Método

Separe fatos por camada:

1. **processo** — o container iniciou e continua vivo?
2. **Pod** — Conditions dizem Ready?
3. **controller** — desejado e disponível convergiram?
4. **seleção** — labels e selector descrevem o mesmo conjunto?
5. **rede** — EndpointSlice tem destinos prontos?
6. **aplicação** — o destino devolve o conteúdo correto?

Não mude três campos antes de medir um. Corrija uma hipótese, reaplique, observe qual
Condition ou objeto mudou e só então avance.

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-14 apply -f setup/stack.yaml
kubectl --context docker-desktop -n learning-infra-k8s-14 rollout status deployment/api
```

## Verifique

Três Asserções independentes cobrem controller, descoberta e resposta. Elas foram
separadas para uma correção parcial produzir informação, não apenas um “falhou”.

## O que você aprendeu

Incidentes atravessam camadas. `Running`, `Ready`, `Available`, endpoint selecionado e
HTTP correto não são sinônimos. Diagnóstico eficiente segue dependências de baixo para
cima, usa estado e Events como evidência e valida cada correção antes da próxima.
