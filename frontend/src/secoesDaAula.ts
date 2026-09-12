import { slugificar } from './texto'

export type SecaoDaAula = { title: string; id: string }

/** Os `##` do Markdown viram a navegação lateral da aula. */
export function secoesDoMarkdown(markdown: string): SecaoDaAula[] {
  return markdown
    .split('\n')
    .filter((linha) => /^##\s+/.test(linha))
    .map((linha) => linha.replace(/^##\s+/, '').trim())
    .map((title) => ({ title, id: slugificar(title) }))
}

export function rolarAteSecao(id: string) {
  document.getElementById(id)?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
  })
}
