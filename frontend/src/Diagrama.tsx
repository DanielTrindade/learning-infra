import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
} from 'react'
import { parse } from 'yaml'
import { IlustracaoDoDiagrama } from './IlustracaoDoDiagrama'
import { visuaisDosDiagramas, type VisualDoDiagrama } from './visuaisDoDiagrama'

/*
 * Diagramas declarativos do conteúdo Markdown.
 *
 * Um bloco ```diagrama traz um objeto YAML com `tipo` e os campos daquele
 * tipo. A renderização é sempre segura: qualquer problema de leitura cai no
 * quadro de erro, que mostra a fonte ao autor em vez de quebrar a página.
 *
 * Tipos: fluxo | camadas | comparacao | ciclo. Ver README ("Escrevendo um
 * Cenário") e ADR 0004.
 */

type Tom = 'neutro' | 'destaque' | 'sucesso' | 'alerta' | 'perigo'

interface NoDeFluxo {
  title: string
  detail?: string
  tom: Tom
  lateral?: { title: string; detail?: string; tom: Tom }
}

interface EspecificacaoBase {
  title?: string
  legenda?: string
  visual?: VisualDoDiagrama
}

interface EspecificacaoFluxo extends EspecificacaoBase {
  tipo: 'fluxo'
  passos: NoDeFluxo[]
}

interface EspecificacaoCamadas extends EspecificacaoBase {
  tipo: 'camadas'
  camadas: Array<{ title: string; detail?: string; tom: Tom }>
}

interface EspecificacaoComparacao extends EspecificacaoBase {
  tipo: 'comparacao'
  colunas: Array<{ title: string; tom: Tom; itens: string[] }>
}

interface EspecificacaoCiclo extends EspecificacaoBase {
  tipo: 'ciclo'
  passos: Array<{ title: string; detail?: string; tom: Tom }>
  retorno?: string
}

type Especificacao =
  | EspecificacaoFluxo
  | EspecificacaoCamadas
  | EspecificacaoComparacao
  | EspecificacaoCiclo

type Interpretacao =
  | { ok: true; especificacao: Especificacao }
  | { ok: false; error: string }

const rotulosDoTipo: Record<Especificacao['tipo'], string> = {
  fluxo: 'sequência',
  camadas: 'camadas',
  comparacao: 'comparação',
  ciclo: 'laço contínuo',
}

const tonsValidos = new Set<Tom>(['neutro', 'destaque', 'sucesso', 'alerta', 'perigo'])
const visuaisValidos = new Set<string>(visuaisDosDiagramas)

function falha(motivo: string): Interpretacao {
  return { ok: false, error: motivo }
}

function textoOpcional(valor: unknown, campo: string): string | undefined {
  if (valor === undefined || valor === null) return undefined
  if (typeof valor !== 'string' || valor.trim() === '') {
    throw new Error(`o campo "${campo}" precisa ser texto`)
  }
  return valor.trim()
}

function textoObrigatorio(valor: unknown, campo: string): string {
  const text = textoOpcional(valor, campo)
  if (text === undefined) throw new Error(`o campo "${campo}" é obrigatório`)
  return text
}

function tomDe(valor: unknown, campo: string): Tom {
  if (valor === undefined || valor === null) return 'neutro'
  if (typeof valor === 'string' && tonsValidos.has(valor as Tom)) return valor as Tom
  throw new Error(
    `o campo "${campo}" precisa ser neutro, destaque, sucesso, alerta ou perigo`,
  )
}

function visualDe(valor: unknown): VisualDoDiagrama | undefined {
  if (valor === undefined || valor === null) return undefined
  if (typeof valor === 'string' && visuaisValidos.has(valor)) {
    return valor as VisualDoDiagrama
  }
  throw new Error(`o campo "visual" não pertence ao catálogo: ${String(valor)}`)
}

function comoLista(valor: unknown, campo: string): Array<Record<string, unknown>> {
  if (!Array.isArray(valor) || valor.length === 0) {
    throw new Error(`o campo "${campo}" precisa ser uma lista com pelo menos um item`)
  }
  return valor.map((item, indice) => {
    if (typeof item !== 'object' || item === null) {
      throw new Error(`o item ${indice + 1} de "${campo}" precisa ser um objeto`)
    }
    return item as Record<string, unknown>
  })
}

function lerNo(item: Record<string, unknown>, campo: string): { title: string; detail?: string; tom: Tom } {
  return {
    title: textoObrigatorio(item.titulo, `${campo}.titulo`),
    detail: textoOpcional(item.detalhe, `${campo}.detalhe`),
    tom: tomDe(item.tom, `${campo}.tom`),
  }
}

function lerBase(dados: Record<string, unknown>): EspecificacaoBase {
  return {
    title: textoOpcional(dados.titulo, 'titulo'),
    legenda: textoOpcional(dados.legenda, 'legenda'),
    visual: visualDe(dados.visual),
  }
}

function interpretar(fonte: string): Interpretacao {
  let bruto: unknown
  try {
    bruto = parse(fonte)
  } catch (e) {
    return falha(`YAML inválido: ${e instanceof Error ? e.message.split('\n')[0] : String(e)}`)
  }
  if (typeof bruto !== 'object' || bruto === null) {
    return falha('o bloco precisa ser um objeto YAML')
  }

  try {
    const dados = bruto as Record<string, unknown>
    const base = lerBase(dados)
    switch (dados.tipo) {
      case 'fluxo':
        return {
          ok: true,
          especificacao: {
            ...base,
            tipo: 'fluxo',
            passos: comoLista(dados.passos, 'passos').map((item, indice) => ({
              ...lerNo(item, `passos[${indice}]`),
              lateral: item.lateral === undefined || item.lateral === null
                ? undefined
                : lerNo(
                    item.lateral as Record<string, unknown>,
                    `passos[${indice}].lateral`,
                  ),
            })),
          },
        }
      case 'camadas':
        return {
          ok: true,
          especificacao: {
            ...base,
            tipo: 'camadas',
            camadas: comoLista(dados.camadas, 'camadas').map((item, indice) =>
              lerNo(item, `camadas[${indice}]`),
            ),
          },
        }
      case 'comparacao':
        return {
          ok: true,
          especificacao: {
            ...base,
            tipo: 'comparacao',
            colunas: comoLista(dados.colunas, 'colunas').map((item, indice) => {
              if (!Array.isArray(item.itens) || item.itens.length === 0) {
                throw new Error(
                  `o campo "colunas[${indice}].itens" precisa ser uma lista com pelo menos um item`,
                )
              }
              return {
                title: textoObrigatorio(item.titulo, `colunas[${indice}].titulo`),
                tom: tomDe(item.tom, `colunas[${indice}].tom`),
                itens: item.itens.map((text, indiceDoItem) =>
                  textoObrigatorio(text, `colunas[${indice}].itens[${indiceDoItem}]`),
                ),
              }
            }),
          },
        }
      case 'ciclo':
        return {
          ok: true,
          especificacao: {
            ...base,
            tipo: 'ciclo',
            passos: comoLista(dados.passos, 'passos').map((item, indice) =>
              lerNo(item, `passos[${indice}]`),
            ),
            retorno: textoOpcional(dados.retorno, 'retorno'),
          },
        }
      default:
        return falha(
          dados.tipo === undefined
            ? 'o campo "tipo" é obrigatório (fluxo, camadas, comparacao ou ciclo)'
            : `tipo desconhecido: "${String(dados.tipo)}"`,
        )
    }
  } catch (e) {
    return falha(e instanceof Error ? e.message : String(e))
  }
}

function useNaViewport<T extends HTMLElement>() {
  const referencia = useRef<T>(null)
  const [visivel, setVisivel] = useState(false)

  useEffect(() => {
    const elemento = referencia.current
    if (
      !elemento ||
      !('IntersectionObserver' in window) ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      setVisivel(true)
      return
    }
    const observador = new IntersectionObserver(
      (entradas) => {
        if (entradas.some((entrada) => entrada.isIntersecting)) {
          setVisivel(true)
          observador.disconnect()
        }
      },
      { rootMargin: '0px 0px -10% 0px', threshold: 0.15 },
    )
    observador.observe(elemento)
    return () => observador.disconnect()
  }, [])

  return { referencia, visivel }
}

function atraso(indice: number): CSSProperties {
  return { '--i': indice } as CSSProperties
}

function No({ title, detail, tom, classe = '' }: {
  title: string
  detail?: string
  tom: Tom
  classe?: string
}) {
  return (
    <div className={`no diagrama-tom-${tom} ${classe}`.trim()}>
      <strong>{title}</strong>
      {detail && <span>{detail}</span>}
    </div>
  )
}

function DiagramaDeFluxo({ passos }: EspecificacaoFluxo) {
  return (
    <ol className="fluxo">
      {passos.map((passo, indice) => (
        <li className="fluxo-item" key={indice} style={atraso(indice)}>
          <span className="fluxo-indice" aria-hidden="true">
            {String(indice + 1).padStart(2, '0')}
          </span>
          <div className={passo.lateral ? 'fluxo-grade com-lateral' : 'fluxo-grade'}>
            <No title={passo.title} detail={passo.detail} tom={passo.tom} />
            {passo.lateral && (
              <>
                <span className="fluxo-derivacao" aria-hidden="true" />
                <No
                  classe="no-lateral"
                  title={passo.lateral.title}
                  detail={passo.lateral.detail}
                  tom={passo.lateral.tom}
                />
              </>
            )}
          </div>
        </li>
      ))}
    </ol>
  )
}

function DiagramaDeCamadas({ camadas }: EspecificacaoCamadas) {
  return (
    <ul className="camadas">
      {camadas.map((camada, indice) => (
        <li key={indice} className="camada" style={atraso(indice)}>
          <No title={camada.title} detail={camada.detail} tom={camada.tom} />
        </li>
      ))}
    </ul>
  )
}

function DiagramaDeComparacao({ colunas }: EspecificacaoComparacao) {
  return (
    <div
      className="comparacao"
      style={{ '--colunas': colunas.length } as CSSProperties}
    >
      {colunas.map((coluna, indice) => (
        <section
          key={indice}
          className={`comparacao-coluna diagrama-tom-${coluna.tom}`}
          style={atraso(indice)}
        >
          <h3>{coluna.title}</h3>
          <ul>
            {coluna.itens.map((item, indiceDoItem) => (
              <li key={indiceDoItem}>{item}</li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}

function DiagramaDeCiclo({ passos, retorno }: EspecificacaoCiclo) {
  return (
    <div className="ciclo" style={{ '--n': passos.length } as CSSProperties}>
      <ol className="ciclo-cadeia">
        {passos.map((passo, indice) => (
          <li className="ciclo-item" key={indice} style={atraso(indice)}>
            <No title={passo.title} detail={passo.detail} tom={passo.tom} />
            {indice < passos.length - 1 && <span className="ciclo-seta" aria-hidden="true" />}
          </li>
        ))}
      </ol>
      <div className="ciclo-retorno" aria-hidden="true" />
      {retorno && (
        <p className="ciclo-retorno-legenda">
          <span aria-hidden="true">↺</span> {retorno}
        </p>
      )}
    </div>
  )
}

export function Diagrama({ fonte }: { fonte: string }) {
  const interpretacao = useMemo(() => interpretar(fonte), [fonte])
  const { referencia, visivel } = useNaViewport<HTMLElement>()

  if (!interpretacao.ok) {
    return (
      <figure className="diagrama diagrama-invalido" data-visivel="true">
        <figcaption className="diagrama-cabecalho">
          <span className="diagrama-tipo">erro no diagrama</span>
        </figcaption>
        <pre className="diagrama-fonte">{fonte.trim()}</pre>
        <p className="diagrama-erro" role="alert">
          Este bloco não virou um diagrama: {interpretacao.error}.
        </p>
      </figure>
    )
  }

  const especificacao = interpretacao.especificacao

  return (
    <figure
      className="diagrama"
      data-tipo={especificacao.tipo}
      data-visivel={visivel}
      ref={referencia}
    >
      <figcaption className="diagrama-cabecalho">
        <span className="diagrama-identificacao">
          <span className="diagrama-tipo">{rotulosDoTipo[especificacao.tipo]}</span>
          {especificacao.title && <span className="diagrama-titulo">{especificacao.title}</span>}
        </span>
        {especificacao.visual && <IlustracaoDoDiagrama visual={especificacao.visual} />}
      </figcaption>
      <div className="diagrama-corpo">
        {especificacao.tipo === 'fluxo' && <DiagramaDeFluxo {...especificacao} />}
        {especificacao.tipo === 'camadas' && <DiagramaDeCamadas {...especificacao} />}
        {especificacao.tipo === 'comparacao' && <DiagramaDeComparacao {...especificacao} />}
        {especificacao.tipo === 'ciclo' && <DiagramaDeCiclo {...especificacao} />}
      </div>
      {especificacao.legenda && <p className="diagrama-legenda">{especificacao.legenda}</p>}
    </figure>
  )
}
