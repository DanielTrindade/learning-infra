---
id: kubernetes/11-agendamento-multino
titulo: Réplicas que não podem cair juntas
dificuldade: assistido
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-11
---
# Réplicas que não podem cair juntas

Duas réplicas no mesmo Node protegem contra falha de processo, não contra falha do Node.
Distribua os Pods por hostname e limite uma interrupção voluntária por vez.

Este Cenário exige o cluster `docker-desktop` com pelo menos **dois Nodes schedulable**.
Confira antes:

```powershell
kubectl --context docker-desktop get nodes -o wide
kubectl --context docker-desktop get nodes -o custom-columns=NAME:.metadata.name,TAINTS:.spec.taints
```

## As forças do scheduler

Scheduler filtra Nodes impossíveis e pontua os restantes. As restrições mais comuns têm
funções diferentes:

- `nodeSelector` exige labels simples do Node;
- node affinity expressa regras sobre Nodes;
- pod affinity aproxima Pods;
- pod anti-affinity separa Pods segundo labels e topologia;
- taints repelem; tolerations permitem considerar o Node, sem obrigar uso.

## Complete a topologia

Abra `distribuicao.yaml`. A regra já seleciona outros Pods `app: api`, mas o
`topologyKey` é propositalmente inválido. Troque pelo label padrão que representa o
hostname do Node.

Depois aplique:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-11 apply -f distribuicao.yaml
kubectl --context docker-desktop -n learning-infra-k8s-11 get pods -o wide --watch
```

Se uma réplica ficar `Pending`, leia Events. Com anti-affinity **required**, não ter um
segundo domínio elegível é uma impossibilidade, não uma lentidão. `preferred` permitiria
colocação conjunta como último recurso — uma troca explícita entre disponibilidade e
capacidade de agendar.

## PodDisruptionBudget não é réplica extra

O mesmo arquivo cria um PDB com `maxUnavailable: 1`. Ele limita **evictions voluntárias**,
como drain de Node. Não protege de queda física, OOM, exclusão direta nem erro de rollout.
Sem réplicas distribuídas e prontas, PDB sozinho não cria disponibilidade.

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-11 get pdb api
```

## Verifique

A Verificação exige duas réplicas disponíveis, anti-affinity obrigatória por hostname e
PDB permitindo no máximo uma indisponível.

## O que você aprendeu

Réplicas só protegem contra o domínio de falha em que estão distribuídas. Affinity decide
colocação, readiness decide disponibilidade e PDB governa parte das interrupções
voluntárias. Restrições fortes demais podem transformar resiliência pretendida em Pods
impossíveis de agendar.
