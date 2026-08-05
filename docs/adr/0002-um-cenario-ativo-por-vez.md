# Um único Cenário Ativo por vez

Cenários sobem ambientes reais na mesma máquina, então sobras de um cenário anterior
podem fazer a Verificação de outro passar sem o leitor ter feito nada — e uma
ferramenta de aprendizado que aprova por engano é pior que nenhuma ferramenta.
Decidimos que existe no máximo um Cenário Ativo: iniciar um Cenário derruba o
ambiente do anterior, e o backend é dono desse ciclo de vida.

## Alternativa rejeitada

Isolar cada Cenário por project name e rede próprios, deixando vários coexistir com
a Verificação escopada ao seu próprio ambiente. Rejeitada porque portas do host
continuam colidindo entre Cenários — o isolamento do Docker não resolve isso — e
porque containers esquecidos se acumulam consumindo memória sem que nada no sistema
seja responsável por recolhê-los.

## Consequências

- Não é possível manter dois Cenários pela metade. Trocar de Cenário perde qualquer
  estado não persistido no repo. Aceitável porque um Cenário é curto e refazível por
  construção.
- A confiabilidade da Verificação depende do teardown ter funcionado. Um teardown
  que falha silenciosamente reintroduz exatamente o problema que esta decisão
  existe para eliminar, então a falha de teardown precisa ser ruidosa.
