import { useEffect, useState, type ReactNode } from 'react'
import Markdown from 'react-markdown'
import {
  buscarCenario,
  iniciarCenario,
  verificarCenario,
  type CenarioDetalhado,
  type ResultadoDaVerificacao,
} from './api'
import { ChecklistDeVerificacao } from './ChecklistDeVerificacao'
import {
  estadoDoCenario,
  idDaTrilha,
  nomeDaTrilha,
  numeroDoCenario,
  rotulosDificuldade,
  rotulosEstado,
  salvarTrilhaAtual,
} from './progresso'

function slugificar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}

function textoDoNo(no: ReactNode): string {
  if (typeof no === 'string' || typeof no === 'number') return String(no)
  if (Array.isArray(no)) return no.map(textoDoNo).join('')
  return ''
}

function secoesDoCenario(markdown: string) {
  return markdown
    .split('\n')
    .filter((linha) => /^##\s+/.test(linha))
    .map((linha) => linha.replace(/^##\s+/, '').trim())
    .map((titulo) => ({ titulo, id: slugificar(titulo) }))
}

export function PaginaDoCenario({ id }: { id: string }) {
  const [cenario, setCenario] = useState<CenarioDetalhado | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [diretorio, setDiretorio] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ResultadoDaVerificacao | null>(null)
  const [ocupado, setOcupado] = useState<'iniciando' | 'verificando' | null>(null)
  const [diretorioCopiado, setDiretorioCopiado] = useState(false)

  useEffect(() => {
    let ativo = true
    setCenario(null)
    setErro(null)
    setDiretorio(null)
    setResultado(null)

    buscarCenario(id)
      .then((dados) => {
        if (ativo) setCenario(dados)
      })
      .catch((e) => {
        if (ativo) setErro(String(e))
      })

    return () => {
      ativo = false
    }
  }, [id])

  async function aoIniciar() {
    setOcupado('iniciando')
    setErro(null)
    setResultado(null)
    try {
      const { diretorioDeTrabalho } = await iniciarCenario(id)
      salvarTrilhaAtual(idDaTrilha(id))
      setDiretorio(diretorioDeTrabalho)
      setCenario((atual) => atual ? { ...atual, ativo: true } : atual)
    } catch (e) {
      setErro(String(e))
    } finally {
      setOcupado(null)
    }
  }

  async function aoVerificar() {
    setOcupado('verificando')
    setErro(null)
    try {
      const novoResultado = await verificarCenario(id)
      setResultado(novoResultado)
      if (novoResultado.concluido) {
        setCenario((atual) => atual ? { ...atual, concluido: true } : atual)
      }
    } catch (e) {
      setErro(String(e))
    } finally {
      setOcupado(null)
    }
  }

  async function copiarDiretorio() {
    if (!diretorio) return
    await navigator.clipboard.writeText(diretorio)
    setDiretorioCopiado(true)
    window.setTimeout(() => setDiretorioCopiado(false), 1800)
  }

  if (erro && !cenario) {
    return (
      <div className="estado-pagina estado-erro" role="alert">
        <span className="estado-icone" aria-hidden="true">!</span>
        <strong>Não foi possível abrir esta aula.</strong>
        <span>Volte ao catálogo ou confira se o backend está rodando.</span>
        <code>{erro}</code>
        <a href="#/">Voltar aos Cenários</a>
      </div>
    )
  }

  if (!cenario) {
    return (
      <div className="estado-pagina" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Preparando o Cenário…</strong>
      </div>
    )
  }

  const estado = estadoDoCenario(cenario)
  const secoes = secoesDoCenario(cenario.markdown)
  const podeVerificar = Boolean(diretorio || cenario.ativo)
  const textoStatus = estado === 'concluido'
    ? 'Todas as verificações já passaram. Você pode revisar ou refazer este laboratório.'
    : estado === 'andamento'
      ? 'O ambiente está ativo. Siga a aula no seu terminal e verifique quando quiser.'
      : 'Inicie o ambiente para preparar o diretório de trabalho desta aula.'

  return (
    <div className="pagina-aula">
      <a className="voltar" href="#/"><span aria-hidden="true">←</span> Todos os Cenários</a>

      <header className="cabecalho-aula">
        <p className="terminal-label">
          <span>Cenário {numeroDoCenario(cenario.id)}</span>
          <i aria-hidden="true">/</i>
          <span>{nomeDaTrilha(cenario.id)}</span>
        </p>
        <h1>{cenario.titulo}</h1>
        <div className="metadados-aula">
          <span className={`status status-${estado}`}><i aria-hidden="true" />{rotulosEstado[estado]}</span>
          <span>{rotulosDificuldade[cenario.dificuldade]}</span>
          <span>{cenario.asercoes.length} {cenario.asercoes.length === 1 ? 'verificação' : 'verificações'}</span>
        </div>
      </header>

      <div className="layout-aula">
        <aside className="lateral-aula" aria-label="Controles e navegação do Cenário">
          <section className="painel-execucao">
            <div className="painel-status">
              <span className={`status status-${estado}`}><i aria-hidden="true" />{rotulosEstado[estado]}</span>
              <h2>{estado === 'concluido' ? 'Laboratório aprovado' : estado === 'andamento' ? 'Ambiente preparado' : 'Pronto para começar?'}</h2>
              <p>{textoStatus}</p>
            </div>

            <div className="acoes">
              <button className="botao botao-primario" onClick={aoIniciar} disabled={ocupado !== null}>
                {ocupado === 'iniciando' && <span className="spinner spinner-botao" aria-hidden="true" />}
                {ocupado === 'iniciando'
                  ? 'Preparando…'
                  : cenario.ativo
                    ? 'Reiniciar ambiente'
                    : cenario.concluido
                      ? 'Refazer laboratório'
                      : 'Iniciar ambiente'}
              </button>
              <button className="botao botao-secundario" onClick={aoVerificar} disabled={ocupado !== null || !podeVerificar}>
                {ocupado === 'verificando' && <span className="spinner spinner-botao" aria-hidden="true" />}
                {ocupado === 'verificando' ? 'Verificando…' : 'Verificar exercício'}
              </button>
            </div>

            {!podeVerificar && (
              <p className="acao-ajuda">A verificação será liberada depois que o ambiente iniciar.</p>
            )}

            {cenario.ativo && !diretorio && (
              <p className="acao-ajuda">Este ambiente já estava ativo. Reinicie apenas se precisar recriar <code>work/</code>.</p>
            )}

            {diretorio && (
              <div className="diretorio">
                <span>Diretório de trabalho</span>
                <code>{diretorio}</code>
                <button type="button" onClick={copiarDiretorio}>
                  {diretorioCopiado ? 'Copiado' : 'Copiar caminho'}
                </button>
              </div>
            )}

            {erro && <p className="erro" role="alert">{erro}</p>}
          </section>

          {resultado && <ChecklistDeVerificacao resultado={resultado} />}

          {secoes.length > 0 && (
            <nav className="sumario" aria-label="Nesta aula">
              <p>Neste Cenário</p>
              <ol>
                {secoes.map((secao, indice) => (
                  <li key={secao.id}>
                    <button
                      type="button"
                      onClick={() => document.getElementById(secao.id)?.scrollIntoView({
                        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
                      })}
                    >
                      <span>{String(indice + 1).padStart(2, '0')}</span>
                      {secao.titulo}
                    </button>
                  </li>
                ))}
              </ol>
            </nav>
          )}
        </aside>

        <article className="conteudo-aula">
          <div className="nota-de-uso">
            <span aria-hidden="true">↗</span>
            <p><strong>Os comandos rodam no seu terminal.</strong> Esta página explica o exercício e verifica o resultado na sua máquina.</p>
          </div>

          <Markdown
            components={{
              h1: () => null,
              h2: ({ children }) => <h2 id={slugificar(textoDoNo(children))}>{children}</h2>,
            }}
          >
            {cenario.markdown}
          </Markdown>
        </article>
      </div>
    </div>
  )
}
