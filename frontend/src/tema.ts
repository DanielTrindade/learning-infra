export type Tema = 'claro' | 'escuro'

const CHAVE_TEMA = 'learning-infra:tema:v1'

/** O tema escolhido à mão, quando existe. `null` significa "siga o sistema". */
export function carregarTema(): Tema | null {
  try {
    const valor = localStorage.getItem(CHAVE_TEMA)
    return valor === 'claro' || valor === 'escuro' ? valor : null
  } catch {
    return null
  }
}

export function salvarTema(tema: Tema) {
  try {
    localStorage.setItem(CHAVE_TEMA, tema)
  } catch {
    // A troca vale para a sessão mesmo quando o browser bloqueia armazenamento local.
  }
}

export function temaDoSistema(): Tema {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro'
}

/**
 * Escreve o tema no <html>. Sem escolha manual o atributo sai do DOM, e as
 * regras de `prefers-color-scheme` voltam a mandar sozinhas.
 */
export function aplicarTema(tema: Tema | null) {
  const raiz = document.documentElement
  if (tema === null) raiz.removeAttribute('data-tema')
  else raiz.setAttribute('data-tema', tema)

  const efetivo = tema ?? temaDoSistema()
  document
    .querySelector('meta[name="theme-color"]')
    ?.setAttribute('content', efetivo === 'escuro' ? '#0c0f11' : '#f4f6f7')
}
