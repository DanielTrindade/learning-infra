import { useEffect, useState } from 'react'
import { listarTrilhas, type TrilhaResumo } from './api'

type EstadoDasTrilhas =
  | { tipo: 'carregando' }
  | { tipo: 'pronto'; trilhas: TrilhaResumo[] }
  | { tipo: 'erro' }

function rolarAteAsTrilhas() {
  document.getElementById('trilhas-disponiveis')?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
  })
}

function PreviaDasTrilhas({ estado }: { estado: EstadoDasTrilhas }) {
  if (estado.tipo === 'carregando') {
    return (
      <div className="landing-trilhas-skeleton" role="status" aria-label="Carregando trilhas">
        {[0, 1, 2, 3].map((item) => <span key={item} />)}
      </div>
    )
  }

  if (estado.tipo === 'erro') {
    return (
      <div className="landing-trilhas-estado" role="alert">
        <strong>O catálogo não respondeu agora.</strong>
        <p>A área de aprendizagem continua disponível para você tentar novamente.</p>
        <a href="#/aprender">Minha aprendizagem</a>
      </div>
    )
  }

  if (estado.trilhas.length === 0) {
    return (
      <div className="landing-trilhas-estado">
        <strong>Novas trilhas estão sendo preparadas.</strong>
        <p>Volte em breve para começar os laboratórios.</p>
      </div>
    )
  }

  return (
    <ol className="landing-trilhas-lista">
      {estado.trilhas.slice(0, 4).map((trilha) => (
        <li key={trilha.id}>
          <div>
            <strong>{trilha.titulo}</strong>
            <span>{trilha.total} {trilha.total === 1 ? 'etapa' : 'etapas'} entre teoria e prática</span>
          </div>
          <a href="#/aprender" aria-label={`Estudar trilha ${trilha.titulo}`}>Estudar trilha</a>
        </li>
      ))}
    </ol>
  )
}

export function LandingPage() {
  const [estadoDasTrilhas, setEstadoDasTrilhas] = useState<EstadoDasTrilhas>({ tipo: 'carregando' })

  useEffect(() => {
    let ativo = true
    listarTrilhas()
      .then((trilhas) => {
        if (ativo) setEstadoDasTrilhas({ tipo: 'pronto', trilhas })
      })
      .catch(() => {
        if (ativo) setEstadoDasTrilhas({ tipo: 'erro' })
      })

    return () => {
      ativo = false
    }
  }, [])

  return (
    <div className="landing-page">
      <section className="landing-hero" aria-labelledby="landing-titulo">
        <div className="landing-hero-copy">
          <h1 id="landing-titulo">Infraestrutura se aprende na prática.</h1>
          <p>Resolva problemas reais em laboratórios locais, com fundamentos claros e verificação automática.</p>
          <div className="landing-hero-acoes">
            <a className="botao botao-primario" href="#/aprender">Começar agora</a>
            <button className="botao botao-texto" type="button" onClick={rolarAteAsTrilhas}>
              Conhecer trilhas
            </button>
          </div>
        </div>
        <figure className="landing-hero-midia">
          <img
            src="/learning-infra-lab-hero.webp"
            width="1122"
            height="1402"
            fetchPriority="high"
            alt="Pequeno laboratório local com mini servidor, cabos, notebook e caderno"
          />
        </figure>
      </section>

      <section className="landing-metodo" id="como-funciona" aria-labelledby="metodo-titulo">
        <div className="landing-secao-introducao">
          <h2 id="metodo-titulo">Do conceito ao diagnóstico.</h2>
          <p>Cada etapa prepara você para entender o sistema, agir no ambiente e confirmar o resultado.</p>
        </div>

        <div className="metodo-grade">
          <article className="metodo-principal">
            <span>Entenda</span>
            <h3>Construa o modelo mental antes do comando.</h3>
            <p>Fundamentos curtos conectam decisões técnicas ao comportamento real da infraestrutura.</p>
          </article>
          <article className="metodo-pratica">
            <span>Pratique</span>
            <h3>Trabalhe em um ambiente local.</h3>
            <p>Os Cenários colocam você diante de uma situação concreta, sem terminal simulado.</p>
          </article>
          <article className="metodo-verificacao">
            <span>Verifique</span>
            <h3>Prove que a solução funciona.</h3>
            <p>A plataforma confere o estado final e mostra exatamente o que precisa ser revisto.</p>
          </article>
        </div>
      </section>

      <section className="landing-pratica" aria-labelledby="pratica-titulo">
        <figure>
          <img
            src="/learning-infra-cable-lab.webp"
            width="1672"
            height="941"
            loading="lazy"
            alt="Pessoa conectando um cabo de rede a um mini servidor em uma bancada"
          />
        </figure>
        <div>
          <h2 id="pratica-titulo">O laboratório roda onde você aprende.</h2>
          <p>Você investiga arquivos, serviços e recursos reais na sua máquina. O progresso fica salvo localmente.</p>
          <dl>
            <div>
              <dt>Fundamentos</dt>
              <dd>Contexto para tomar decisões melhores.</dd>
            </div>
            <div>
              <dt>Cenários</dt>
              <dd>Problemas práticos com objetivos claros.</dd>
            </div>
            <div>
              <dt>Verificação</dt>
              <dd>Feedback baseado no estado do ambiente.</dd>
            </div>
          </dl>
        </div>
      </section>

      <section className="landing-trilhas" id="trilhas-disponiveis" aria-labelledby="trilhas-titulo">
        <div className="landing-secao-introducao">
          <h2 id="trilhas-titulo">Escolha sua próxima trilha.</h2>
          <p>Comece por uma base conhecida ou avance para o tema que já aparece no seu trabalho.</p>
        </div>
        <PreviaDasTrilhas estado={estadoDasTrilhas} />
      </section>

      <section className="landing-cta" aria-labelledby="cta-titulo">
        <div>
          <h2 id="cta-titulo">Conhecimento técnico nasce da prática.</h2>
          <p>Escolha uma trilha e resolva o primeiro Cenário no seu próprio ambiente.</p>
        </div>
        <a className="botao botao-primario" href="#/aprender">Começar agora</a>
      </section>

      <footer className="landing-footer">
        <strong>Learning Infra</strong>
        <span>Laboratórios práticos de infraestrutura.</span>
        <a href="#/aprender">Minha aprendizagem</a>
      </footer>
    </div>
  )
}
