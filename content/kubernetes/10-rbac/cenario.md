---
id: kubernetes/10-rbac
titulo: Identidade e processo com o mínimo necessário
dificuldade: assistido
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-10
---
# Uma identidade com o mínimo necessário

Um processo de auditoria precisa listar e observar Pods, mas não pode criar, alterar nem
apagar nada. Modele essa autorização com ServiceAccount, Role e RoleBinding, e execute o
processo sem privilégios de sistema operacional desnecessários.

## Autenticação não é autorização

ServiceAccount dá identidade a processos em Pods. RBAC decide o que essa identidade pode
fazer. O fluxo passa por quatro perguntas: quem é, qual verbo, qual recurso e em qual
escopo.

Abra `acesso.yaml`. A identidade e a ligação estão prontas; complete `verbs` da Role para
permitir exatamente `get`, `list` e `watch` sobre `pods`.

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-10 apply -f acesso.yaml
```

## Teste autorização sem fabricar token

Como administrador do cluster local, use impersonação:

```powershell
kubectl --context docker-desktop auth can-i get pods --as=system:serviceaccount:learning-infra-k8s-10:auditor -n learning-infra-k8s-10
kubectl --context docker-desktop auth can-i delete pods --as=system:serviceaccount:learning-infra-k8s-10:auditor -n learning-infra-k8s-10
kubectl --context docker-desktop auth can-i --list --as=system:serviceaccount:learning-infra-k8s-10:auditor -n learning-infra-k8s-10
```

O primeiro deve dizer `yes`; o segundo, `no`. Não crie token persistente para testar.
Tokens projetados em Pods são temporários; credenciais longas ampliam risco sem ajudar
este exercício.

## Duas camadas de privilégio

RBAC governa chamadas à API. `securityContext` governa o processo dentro do container.
O Deployment `auditor` combina controles compatíveis com um perfil restrito:

- UID diferente de root;
- seccomp `RuntimeDefault`;
- `allowPrivilegeEscalation: false`;
- filesystem raiz somente leitura;
- remoção de todas as Linux capabilities.

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-10 rollout status deployment/auditor
kubectl --context docker-desktop -n learning-infra-k8s-10 exec deployment/auditor -- id
```

Um Pod seguro não corrige RBAC amplo, e RBAC mínimo não corrige container privilegiado.
São fronteiras diferentes.

## Role e ClusterRole

Role é namespaced. ClusterRole pode descrever recursos do cluster ou ser reutilizada em
vários namespaces, mas a abrangência final depende do binding. Aqui uma Role é a escolha
mais estreita: o auditor só precisa deste namespace.

Evite curingas em `verbs` e `resources`. “Funciona” não é o critério de autorização;
menor privilégio é dar exatamente o conjunto requerido e verificar também a negação.

## Verifique

A Verificação confirma que `get` é permitido, `delete` é negado, o workload está disponível
e os controles essenciais de execução permanecem ativos.

## O que você aprendeu

ServiceAccount identifica workload; Role descreve permissões locais; RoleBinding conecta
sujeito e papel. `kubectl auth can-i` testa a decisão. `securityContext` reduz o poder do
processo. Segurança útil testa permissão e negação em mais de uma camada.
