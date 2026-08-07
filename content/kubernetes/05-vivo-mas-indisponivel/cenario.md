---
id: kubernetes/05-vivo-mas-indisponivel
titulo: Vivo, mas fora do tráfego
dificuldade: assistido
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-05
manifestosIniciais: setup
---
# Vivo, mas fora do tráfego

Dois Pods estão `Running`, o processo nginx está vivo, e mesmo assim o Service não tem
backends prontos. O ambiente foi aplicado de `setup/stack.yaml`; encontre a condição que
mantém a aplicação fora do tráfego e corrija-a.

## Três perguntas de saúde

Kubernetes separa problemas que costumam ser confundidos:

- **startup probe**: a aplicação já terminou de iniciar?
- **liveness probe**: o processo travou e precisa ser reiniciado?
- **readiness probe**: este Pod pode receber tráfego agora?

Falha de liveness reinicia container. Falha de readiness apenas remove o endpoint dos
Services. Reiniciar uma aplicação que só está temporariamente indisponível pode piorar a
falha; por isso as probes não são intercambiáveis.

## Siga a cadeia de evidências

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-05 get pods
kubectl --context docker-desktop -n learning-infra-k8s-05 describe pod -l app=web
kubectl --context docker-desktop -n learning-infra-k8s-05 get endpointslice -l kubernetes.io/service-name=web -o yaml
kubectl --context docker-desktop -n learning-infra-k8s-05 logs deployment/web
```

`Running` é fase do Pod, não promessa de prontidão. Em `describe`, leia Conditions e
Events antes de alterar qualquer coisa.

## Teste a hipótese dentro do Pod

Compare o caminho configurado na readiness probe com caminhos que o nginx realmente
serve:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-05 exec deployment/web -- wget -S -O- http://127.0.0.1/
kubectl --context docker-desktop -n learning-infra-k8s-05 exec deployment/web -- wget -S -O- http://127.0.0.1/pronto
```

Corrija `setup/stack.yaml`, reaplique e acompanhe:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-05 apply -f setup/stack.yaml
kubectl --context docker-desktop -n learning-infra-k8s-05 rollout status deployment/web
```

## Verifique

As duas réplicas devem ficar disponíveis e o cliente deve alcançar o Service. Corrigir
apenas o Service não resolve uma readiness probe que reprova todos os Pods.

## O que você aprendeu

Fase, processo vivo e endpoint pronto são estados diferentes. Readiness governa entrada
no balanceamento; liveness governa reinício; startup protege inicialização lenta. Events,
Conditions e um teste local dentro do Pod formam uma sequência de diagnóstico melhor do
que editar YAML por tentativa.
