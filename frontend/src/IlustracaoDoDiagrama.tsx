import type { ReactNode } from 'react'
import type { VisualDoDiagrama } from './visuaisDoDiagrama'

const desenhos: Record<VisualDoDiagrama, ReactNode> = {
  'fronteiras-runtime': (
    <>
      <rect className="ilustracao-plano" x="8" y="12" width="62" height="66" rx="4" />
      <path className="ilustracao-linha" d="M8 58h62" />
      <rect className="ilustracao-no" x="16" y="20" width="46" height="30" rx="3" />
      <path className="ilustracao-linha-fantasma" d="M22 28h34M22 35h34M22 42h20" />
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="86" y="12" width="66" height="66" rx="4" />
      <path className="ilustracao-linha" d="M86 58h66" />
      <rect className="ilustracao-no-destaque" x="94" y="22" width="22" height="28" rx="3" />
      <rect className="ilustracao-no-destaque" x="122" y="22" width="22" height="28" rx="3" />
      <circle className="ilustracao-ponto" cx="105" cy="36" r="3" />
      <circle className="ilustracao-ponto" cx="133" cy="36" r="3" />
      <path className="ilustracao-base" d="M12 68h54M90 68h58" />
    </>
  ),
  'motor-docker': (
    <>
      <path className="ilustracao-linha" d="M14 48h128" />
      <path className="ilustracao-linha-fantasma" d="M78 48V18h48" />
      <circle className="ilustracao-no" cx="20" cy="48" r="8" />
      <rect className="ilustracao-no" x="44" y="39" width="18" height="18" rx="3" />
      <rect className="ilustracao-no-destaque" x="78" y="35" width="26" height="26" rx="4" />
      <circle className="ilustracao-ponto" cx="91" cy="48" r="4" />
      <rect className="ilustracao-no" x="120" y="39" width="18" height="18" rx="3" />
      <path className="ilustracao-seta" d="m137 44 7 4-7 4" />
      <path className="ilustracao-plano" d="M118 10h18l6 6v12h-24z" />
      <path className="ilustracao-linha-fantasma" d="M124 17h12M124 22h12" />
    </>
  ),
  'filesystem-camadas': (
    <>
      <path className="ilustracao-plano" d="m24 65 52-14 60 14-52 16z" />
      <path className="ilustracao-plano" d="m30 50 48-13 54 13-48 15z" />
      <path className="ilustracao-plano ilustracao-plano-destaque" d="m38 35 42-11 46 11-42 13z" />
      <path className="ilustracao-plano ilustracao-plano-alerta" d="m47 22 35-9 36 9-34 10z" />
      <path className="ilustracao-linha-fantasma" d="M84 32v49M126 35v15M132 50v15M136 65v1" />
      <circle className="ilustracao-ponto-alerta" cx="84" cy="22" r="3" />
    </>
  ),
  'rotas-container': (
    <>
      <circle className="ilustracao-no" cx="18" cy="31" r="8" />
      <path className="ilustracao-linha" d="M26 31h34" />
      <rect className="ilustracao-plano" x="60" y="17" width="36" height="28" rx="4" />
      <circle className="ilustracao-ponto" cx="78" cy="31" r="4" />
      <path className="ilustracao-linha" d="M96 31h36" />
      <rect className="ilustracao-no-destaque" x="132" y="22" width="18" height="18" rx="3" />
      <path className="ilustracao-linha-fantasma" d="M78 45v25h54" />
      <rect className="ilustracao-no-sucesso" x="132" y="61" width="18" height="18" rx="3" />
      <path className="ilustracao-seta" d="m127 66 7 4-7 4M127 27l7 4-7 4" />
    </>
  ),
  reconciliacao: (
    <>
      <path className="ilustracao-linha" d="M46 20C79 2 126 17 132 48" />
      <path className="ilustracao-linha" d="M126 70C94 91 47 78 34 51" />
      <path className="ilustracao-seta" d="m126 42 7 7-9 2M40 58l-7-8 9-1" />
      <circle className="ilustracao-no" cx="38" cy="35" r="12" />
      <circle className="ilustracao-no-destaque" cx="82" cy="18" r="12" />
      <circle className="ilustracao-no-sucesso" cx="126" cy="61" r="12" />
      <path className="ilustracao-linha-fantasma" d="M32 35h12M76 18h12M120 61h12" />
      <circle className="ilustracao-ponto" cx="82" cy="60" r="4" />
    </>
  ),
  'arquitetura-cluster': (
    <>
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="42" y="8" width="76" height="27" rx="4" />
      <circle className="ilustracao-ponto" cx="55" cy="21.5" r="3" />
      <path className="ilustracao-linha-fantasma" d="M66 17h40M66 24h28" />
      <path className="ilustracao-linha" d="M80 35v15M36 50h88M36 50v13M124 50v13" />
      <rect className="ilustracao-plano" x="12" y="63" width="48" height="24" rx="4" />
      <rect className="ilustracao-plano" x="100" y="63" width="48" height="24" rx="4" />
      <rect className="ilustracao-no" x="20" y="70" width="13" height="10" rx="2" />
      <rect className="ilustracao-no" x="39" y="70" width="13" height="10" rx="2" />
      <rect className="ilustracao-no-sucesso" x="108" y="70" width="13" height="10" rx="2" />
      <rect className="ilustracao-no" x="127" y="70" width="13" height="10" rx="2" />
    </>
  ),
  'hierarquia-workload': (
    <>
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="55" y="8" width="50" height="20" rx="3" />
      <path className="ilustracao-linha" d="M80 28v14" />
      <rect className="ilustracao-plano" x="55" y="42" width="50" height="20" rx="3" />
      <path className="ilustracao-linha" d="M80 62v10M28 72h104M28 72v9M80 72v9M132 72v9" />
      <rect className="ilustracao-no" x="18" y="81" width="20" height="10" rx="2" />
      <rect className="ilustracao-no-sucesso" x="70" y="81" width="20" height="10" rx="2" />
      <rect className="ilustracao-no" x="122" y="81" width="20" height="10" rx="2" />
      <path className="ilustracao-linha-fantasma" d="M64 18h32M64 52h32" />
    </>
  ),
  'service-endpoints': (
    <>
      <circle className="ilustracao-no" cx="18" cy="48" r="9" />
      <path className="ilustracao-linha" d="M27 48h36" />
      <path className="ilustracao-no-destaque" d="m80 31 17 17-17 17-17-17z" />
      <path className="ilustracao-linha" d="M97 48h18M115 22v52M115 22h22M115 48h22M115 74h22" />
      <circle className="ilustracao-no-sucesso" cx="143" cy="22" r="7" />
      <circle className="ilustracao-no-sucesso" cx="143" cy="48" r="7" />
      <circle className="ilustracao-no" cx="143" cy="74" r="7" />
      <path className="ilustracao-linha-fantasma" d="M74 48h12" />
    </>
  ),
  'responsabilidade-aws': (
    <>
      <path className="ilustracao-plano" d="M80 7 133 24v28c0 18-22 30-53 38-31-8-53-20-53-38V24z" />
      <path className="ilustracao-linha" d="M80 8v78M29 49h102" />
      <path className="ilustracao-plano-destaque" d="M80 9 131 25v23H80z" />
      <circle className="ilustracao-ponto" cx="54" cy="34" r="5" />
      <path className="ilustracao-linha-fantasma" d="M43 45h23M92 34h25M92 66h22M43 66h23" />
      <circle className="ilustracao-ponto-alerta" cx="106" cy="58" r="4" />
    </>
  ),
  'fronteiras-aws': (
    <>
      <rect className="ilustracao-plano" x="9" y="8" width="142" height="80" rx="5" />
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="25" y="19" width="110" height="58" rx="4" />
      <rect className="ilustracao-plano" x="42" y="30" width="76" height="36" rx="3" />
      <circle className="ilustracao-no-sucesso" cx="60" cy="48" r="7" />
      <circle className="ilustracao-no" cx="82" cy="48" r="7" />
      <circle className="ilustracao-no" cx="104" cy="48" r="7" />
      <path className="ilustracao-linha-fantasma" d="M17 16h26M33 27h24M50 38h18" />
    </>
  ),
  'planos-aws': (
    <>
      <circle className="ilustracao-no" cx="17" cy="24" r="8" />
      <path className="ilustracao-linha" d="M25 24h30" />
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="55" y="10" width="88" height="28" rx="4" />
      <path className="ilustracao-linha-fantasma" d="M67 19h64M67 28h43" />
      <path className="ilustracao-linha" d="M99 38v20" />
      <rect className="ilustracao-plano" x="55" y="58" width="88" height="28" rx="4" />
      <circle className="ilustracao-no-sucesso" cx="72" cy="72" r="6" />
      <circle className="ilustracao-no-sucesso" cx="99" cy="72" r="6" />
      <circle className="ilustracao-no-alerta" cx="126" cy="72" r="6" />
      <path className="ilustracao-seta" d="m94 52 5 7 5-7" />
    </>
  ),
  'fidelidade-local': (
    <>
      <path className="ilustracao-plano ilustracao-plano-destaque" d="M12 18h38v60H12z" />
      <path className="ilustracao-plano" d="M61 28h38v50H61z" />
      <path className="ilustracao-plano ilustracao-plano-alerta" d="M110 40h38v38h-38z" />
      <circle className="ilustracao-no-sucesso" cx="31" cy="38" r="7" />
      <path className="ilustracao-linha" d="M20 54h22M20 63h22" />
      <circle className="ilustracao-no-destaque" cx="80" cy="46" r="7" />
      <path className="ilustracao-linha-fantasma" d="M69 61h22M69 69h14" />
      <circle className="ilustracao-no-alerta" cx="129" cy="55" r="7" />
      <path className="ilustracao-linha-fantasma" d="M118 69h22" />
      <path className="ilustracao-base" d="M7 84h146" />
    </>
  ),
}

export function IlustracaoDoDiagrama({ visual }: { visual: VisualDoDiagrama }) {
  return (
    <svg
      aria-hidden="true"
      className="diagrama-ilustracao"
      focusable="false"
      viewBox="0 0 160 96"
    >
      {desenhos[visual]}
    </svg>
  )
}
