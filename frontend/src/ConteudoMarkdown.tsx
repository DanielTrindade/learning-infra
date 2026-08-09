import { isValidElement, type ReactNode } from 'react'
import Markdown from 'react-markdown'
import { BlocoDeCodigo } from './BlocoDeCodigo'
import { Diagrama } from './Diagrama'
import { slugificar, textoDoNo } from './texto'

function PreDoConteudo({ children }: { children?: ReactNode }) {
  if (isValidElement<{ className?: string; children?: ReactNode }>(children)) {
    const { className, children: conteudo } = children.props
    if (typeof className === 'string' && /(?:^|\s)language-diagrama(?:\s|$)/.test(className)) {
      return <Diagrama fonte={textoDoNo(conteudo)} />
    }
  }
  return <BlocoDeCodigo>{children}</BlocoDeCodigo>
}

export function ConteudoMarkdown({ markdown }: { markdown: string }) {
  return (
    <Markdown
      components={{
        h1: () => null,
        h2: ({ children }) => <h2 id={slugificar(textoDoNo(children))}>{children}</h2>,
        pre: PreDoConteudo,
      }}
    >
      {markdown}
    </Markdown>
  )
}
