/**
 * Cada Trilha ganha um glifo próprio para que o catálogo seja varrido pelo
 * formato, não pela leitura do título. São marcas geométricas simples, não
 * logotipos das ferramentas — o projeto não redistribui marca de terceiro.
 */
const glifos: Record<string, string> = {
  docker: '▤',
  kubernetes: '⎈',
  aws: '◈',
  iac: '⌗',
  linux: '❯',
}

export function glifoDaTrilha(id: string): string {
  return glifos[id] ?? '▣'
}
