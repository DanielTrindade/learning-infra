---
id: kubernetes/04-configuracao
titulo: Configuração sem reconstruir imagem
dificuldade: guiado
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-04
---
# Configuração sem reconstruir imagem

Imagem deve carregar aplicação; configuração deve variar por ambiente. Neste Cenário o
mesmo nginx recebe uma página por ConfigMap e um token por Secret, sem novo `docker build`.

## Três objetos, três responsabilidades

Abra `stack.yaml`:

- ConfigMap guarda a página não sensível;
- Secret recebe `stringData`, que a API converte para `data` em base64;
- Deployment injeta um valor como variável e outro como arquivo montado.

Base64 é codificação, **não criptografia**. Secret controla a forma de distribuição e
integra com autorização, mas proteção em repouso depende da configuração do cluster.

## Aplique e observe

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-04 apply -f stack.yaml
kubectl --context docker-desktop -n learning-infra-k8s-04 rollout status deployment/web
kubectl --context docker-desktop -n learning-infra-k8s-04 get configmap pagina -o yaml
kubectl --context docker-desktop -n learning-infra-k8s-04 get secret credencial -o yaml
```

Confirme de dentro do container:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-04 exec deployment/web -- cat /usr/share/nginx/html/index.html
kubectl --context docker-desktop -n learning-infra-k8s-04 exec deployment/web -- printenv LAB_TOKEN
```

## Atualização não significa recarga

Altere o texto da ConfigMap e reaplique. O arquivo montado é atualizado depois de um
intervalo, mas aplicações podem manter conteúdo em memória. Variáveis de ambiente nunca
mudam num processo já iniciado: exigem Pod novo.

Force uma nova revisão do Deployment quando a configuração consumida no processo mudar:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-04 rollout restart deployment/web
kubectl --context docker-desktop -n learning-infra-k8s-04 rollout status deployment/web
```

Em sistemas reais, costuma-se colocar o hash da configuração no template para o rollout
acontecer declarativamente.

## Verifique

A Verificação lê arquivo e variável dentro do Pod. Ela não considera a existência dos
objetos suficiente: configuração só tem valor quando a carga a consome.

## O que você aprendeu

ConfigMap separa configuração não sensível; Secret distribui material sensível sem o
transformar em segredo criptográfico automaticamente. Volume pode atualizar arquivo;
variável de ambiente fica congelada no nascimento do processo. Alterar objeto e fazer a
aplicação perceber a alteração são problemas diferentes.
