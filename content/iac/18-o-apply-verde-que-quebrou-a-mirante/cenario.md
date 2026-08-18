---
id: iac/18-o-apply-verde-que-quebrou-a-mirante
titulo: O apply verde que quebrou a Mirante
dificuldade: mestre
terraform: true
containers: [mirante-homologacao, mirante-producao]
volumes: [mirante-estoque]
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-iac-18
ministack: true
---
# O apply verde que quebrou a Mirante

> Nota do projeto: este Cenário mantém os três substratos no ar ao mesmo tempo — Docker,
> Kubernetes e a AWS local do MiniStack. Nada foi cortado; tudo está junto nesta sala.

A sexta-feira terminou em verde. O `apply` rodou, o plano foi aprovado, o mundo foi
escrito — e nenhuma linha saiu errada. No fim de semana a infraestrutura da Mirante foi
mexida e aplicada de novo, com o mesmo resultado: verde, sem um erro. Na segunda de manhã,
a Mirante estava quebrada em quatro lugares — e nenhum deles tinha dito que quebraria.
Nada falhou; tudo aplicou. A pergunta desta segunda não é "o que deu errado": o plano não
acusou erro nenhum. É **o que um apply verde não prova**.

Este Cenário é `Mestre`: o ambiente chega quebrado e a causa não é revelada. O texto fala
pouco de propósito. O trabalho é descobrir.

## O ambiente

Rode o script que deixa a Mirante no estado em que ela acordou na segunda:

```powershell
.\preparar.ps1
```

Nada além disso. O diretório de trabalho já vem com o que a sexta-feira deixou e com o que
o fim de semana escreveu — leia o que chegou antes de mexer em qualquer coisa.

## O objetivo

Ao final deste Cenário:

- os dois ambientes respondem **cada um na sua porta** — a homologação na **8072**, a
  produção na **8071**;
- o volume de estoque **preserva o dado que já estava lá**;
- o cluster tem o número de réplicas que o código declara;
- a fila da Mirante está **sob gestão do Terraform**;
- e `terraform plan` sai limpo.

## A única dica

O plano é o instrumento. Quatro problemas diferentes deixam **quatro marcas diferentes**
no plano — e todas aparecem antes de qualquer `apply`.

## O inventário de fidelidade

Para fechar a Trilha e a plataforma, escreva — com as suas palavras — o que este
laboratório **provou** e o que ele **não prova**.

De um lado, o que foi provado localmente, com fidelidade real: o ciclo de vida de um
container acontece de verdade — processo executa, porta responde, dado sobrevive — e os
objetos de Kubernetes são objetos de verdade — o API server os registra e os controllers
os reconciliam. Aqui, o verde significa alguma coisa.

Do outro, o que continua exigindo uma conta sandbox: **IAM aplicado** — este laboratório
aceita credenciais sintéticas sem verificar identidade alguma; **isolamento de rede** —
tudo roda num processo só, no loopback, sem tráfego real a filtrar; **custo** — nada é
cobrado; **disponibilidade** — não há zonas de disponibilidade nem failover; e
**comportamento sob carga** — uma réplica a mais num cluster local não é uma promoção
nacional.

E feche o inventário com a frase que resume as quatro Trilhas: **saber o que o seu
laboratório não prova é parte de saber operar infraestrutura.**
