/**
 * Ilustração do hero: rack isométrico sobre a malha de planta baixa.
 * Vinha de um .svg com o violeta antigo cravado; agora é inline para que cada
 * traço leia os tokens de tema e a peça funcione no claro e no escuro.
 */
export function IlustracaoDoHero() {
  return (
    <svg
      className="hero-ilustracao"
      viewBox="0 0 400 400"
      width="400"
      height="400"
      fill="none"
      aria-hidden="true"
    >
  <defs>
    <radialGradient id="hero-esmaecer" cx="50%" cy="72%" r="46%">
      <stop offset="0%" stopColor="#fff"></stop>
      <stop offset="55%" stopColor="#fff" stopOpacity=".8"></stop>
      <stop offset="100%" stopColor="#fff" stopOpacity="0"></stop>
    </radialGradient>
    <mask id="hero-mascara"><rect width="400" height="400" fill="url(#hero-esmaecer)"></rect></mask>
  </defs>
  <g mask="url(#hero-mascara)"><path d="M200 215L50 300M200 215L350 300M230 232L80 317M170 232L320 317M260 249L110 334M140 249L290 334M290 266L140 351M110 266L260 351M320 283L170 368M80 283L230 368M350 300L200 385M50 300L200 385" className="hero-malha" strokeWidth="1"></path></g>
  <path d="M200 215L350 300L200 385L50 300Z" className="hero-base" strokeWidth="1.1" strokeLinejoin="round"></path>
  <path d="M200 252.4L272 294.9M200 252.4L131 300M200 252.4L197 344.2" className="hero-enlace" strokeWidth="1.2" strokeDasharray="4 5"></path>
  <g className="hero-no" strokeWidth="1.2"><circle cx="200" cy="252.4" r="3.4"></circle><circle cx="272" cy="294.9" r="3.4"></circle><circle cx="131" cy="300" r="3.4"></circle><circle cx="197" cy="344.2" r="3.4"></circle></g>
  <g className="hero-rack" strokeWidth="1.2" strokeLinejoin="round">
    <path d="M155 168.4L155 252.4L200 277.9L200 193.9Z" className="hero-face"></path>
    <path d="M200 193.9L200 277.9L245 252.4L245 168.4Z" className="hero-face-lateral"></path>
    <path d="M155 238.4L200 263.9M200 263.9L245 238.4M155 224.4L200 249.9M200 249.9L245 224.4M155 210.4L200 235.9M200 235.9L245 210.4M155 196.4L200 221.9M200 221.9L245 196.4M155 182.4L200 207.9M200 207.9L245 182.4" strokeOpacity="0.35" strokeWidth="1"></path>
    <path d="M200 142.9L245 168.4L200 193.9L155 168.4Z" className="hero-face-topo"></path>
  </g><g className="hero-caixa" strokeWidth="1.2" strokeLinejoin="round">
    <path d="M233 244.9L233 294.9L272 317L272 267Z" className="hero-face"></path>
    <path d="M272 267L272 317L311 294.9L311 244.9Z" className="hero-face"></path>
    <path d="M233 282.4L272 304.5M272 304.5L311 282.4M233 269.9L272 292M272 292L311 269.9M233 257.4L272 279.5M272 279.5L311 257.4" strokeOpacity="0.22" strokeWidth="1"></path>
    <path d="M272 222.8L311 244.9L272 267L233 244.9Z" className="hero-face"></path>
  </g><g className="hero-caixa" strokeWidth="1.2" strokeLinejoin="round">
    <path d="M92 258L92 300L131 322.1L131 280.1Z" className="hero-face"></path>
    <path d="M131 280.1L131 322.1L170 300L170 258Z" className="hero-face"></path>
    <path d="M92 286L131 308.1M131 308.1L170 286M92 272L131 294.1M131 294.1L170 272" strokeOpacity="0.22" strokeWidth="1"></path>
    <path d="M131 235.9L170 258L131 280.1L92 258Z" className="hero-face"></path>
  </g><g className="hero-caixa" strokeWidth="1.2" strokeLinejoin="round">
    <path d="M152 278.2L152 344.2L197 369.7L197 303.7Z" className="hero-face"></path>
    <path d="M197 303.7L197 369.7L242 344.2L242 278.2Z" className="hero-face"></path>
    <path d="M152 331L197 356.5M197 356.5L242 331M152 317.8L197 343.3M197 343.3L242 317.8M152 304.6L197 330.1M197 330.1L242 304.6M152 291.4L197 316.9M197 316.9L242 291.4" strokeOpacity="0.22" strokeWidth="1"></path>
    <path d="M197 252.7L242 278.2L197 303.7L152 278.2Z" className="hero-face"></path>
  </g>
  <path d="M200 168.4L200 134.4" className="hero-enlace" strokeWidth="1" strokeDasharray="3 4"></path>
  <path d="M200 98L218 108.2L200 118.4L182 108.2ZM182 108.2L182 124.2L200 134.4L200 118.4ZM200 118.4L200 134.4L218 124.2L218 108.2ZM219.5 87.05L237.5 97.25L219.5 107.45L201.5 97.25ZM201.5 97.25L201.5 113.25L219.5 123.45L219.5 107.45ZM219.5 107.45L219.5 123.45L237.5 113.25L237.5 97.25ZM180.5 87.05L198.5 97.25L180.5 107.45L162.5 97.25ZM162.5 97.25L162.5 113.25L180.5 123.45L180.5 107.45ZM180.5 107.45L180.5 123.45L198.5 113.25L198.5 97.25Z" className="hero-cubo" strokeWidth="1.2" strokeLinejoin="round"></path>
    </svg>
  )
}
