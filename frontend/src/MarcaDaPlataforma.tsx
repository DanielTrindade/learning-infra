/**
 * Marca da plataforma: três camadas isométricas empilhadas — a leitura literal
 * de "infraestrutura". Herda a cor do tema em vez de trazer a sua, para
 * funcionar igual no claro e no escuro.
 */
export function MarcaDaPlataforma() {
  return (
    <svg className="marca-simbolo" viewBox="0 0 32 32" width="32" height="32" aria-hidden="true">
      <rect width="32" height="32" rx="8" className="marca-fundo" />
      <g className="marca-traco" fill="none" strokeWidth="1.7" strokeLinejoin="round">
        <path d="M16 7.5 24 12l-8 4.5-8-4.5Z" />
        <path d="M8 16l8 4.5 8-4.5" />
        <path d="M8 20l8 4.5 8-4.5" />
      </g>
    </svg>
  )
}
