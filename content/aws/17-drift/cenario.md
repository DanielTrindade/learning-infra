---
id: aws/17-drift
titulo: O recurso mudou fora do template
dificuldade: mestre
ministack: true
inicializacaoAws: init
---
# O recurso mudou fora do template

O ambiente inicia com um stack que cria a fila `learning-infra-drift` com visibility
timeout de apenas 5 segundos. O arquivo `stack.yaml` representa a correção desejada:
45 segundos e uma tag de propriedade.

## Incidente

Alguém corrigiu a fila manualmente no console/CLI, mas não atualizou a fonte declarativa.
Na próxima recriação, o timeout voltou a 5. Sua tarefa é fazer o template voltar a ser a
fonte de verdade.

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint cloudformation describe-stacks `
  --stack-name learning-infra-drift
aws --endpoint-url $endpoint cloudformation deploy `
  --stack-name learning-infra-drift --template-file stack.yaml
```

Não resolva apenas com `set-queue-attributes`: isso melhora o estado observado agora,
mas preserva a divergência que causou o incidente.

## O que observar

Drift é diferença entre intenção e realidade. O MiniStack não cobre todas as APIs de
detecção de drift, mas permite reproduzir a causa: mutação imperativa que não volta ao
artefato declarativo.

