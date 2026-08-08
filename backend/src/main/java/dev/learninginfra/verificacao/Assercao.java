package dev.learninginfra.verificacao;

public sealed interface Assercao {

    String descricao();

    record ContainerRodando(String nome) implements Assercao {
        @Override
        public String descricao() {
            return "o container `" + nome + "` está rodando";
        }
    }

    record HttpResponde(String url, int status) implements Assercao {
        @Override
        public String descricao() {
            return "GET " + url + " responde " + status;
        }
    }

    record HttpCorpoContem(String url, String texto) implements Assercao {
        @Override
        public String descricao() {
            return "o corpo de " + url + " contém \"" + texto + "\"";
        }
    }

    record ImagemExiste(String referencia) implements Assercao {
        @Override
        public String descricao() {
            return "a imagem `" + referencia + "` existe localmente";
        }
    }

    record VolumeExiste(String nome) implements Assercao {
        @Override
        public String descricao() {
            return "o volume `" + nome + "` existe";
        }
    }

    record ContainerSaudavel(String nome) implements Assercao {
        @Override
        public String descricao() {
            return "o container `" + nome + "` está saudável";
        }
    }

    record ContainerEmRede(String nome, String rede, boolean presente) implements Assercao {
        @Override
        public String descricao() {
            return presente
                    ? "o container `" + nome + "` está na rede `" + rede + "`"
                    : "o container `" + nome + "` não está na rede `" + rede + "`";
        }
    }

    /**
     * O escape hatch do vocabulário: roda um comando e confere a saída. A descrição vem
     * do Cenário, e não do comando, porque o comando costuma revelar a resposta do
     * exercício. O acessor do componente já implementa {@link Assercao#descricao()}.
     */
    record ComandoProduz(java.util.List<String> comando, String contem, String descricao)
            implements Assercao {
    }

    record KubernetesCondicao(
            String contexto,
            String namespace,
            String recurso,
            String nome,
            String condicao,
            String status,
            int timeoutSegundos,
            String descricao) implements Assercao {
    }

    record KubernetesJsonpath(
            String contexto,
            String namespace,
            String recurso,
            String nome,
            String expressao,
            String contem,
            int timeoutSegundos,
            String descricao) implements Assercao {
    }

    record KubernetesRbac(
            String contexto,
            String namespace,
            String serviceAccount,
            String verbo,
            String recurso,
            boolean permitido,
            String descricao) implements Assercao {
    }

    record AwsConsulta(
            String endpoint,
            String regiao,
            String servico,
            String operacao,
            java.util.List<String> argumentos,
            String consulta,
            String esperado,
            String descricao) implements Assercao {
    }
}
