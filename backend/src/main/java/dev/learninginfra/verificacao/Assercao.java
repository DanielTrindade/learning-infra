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
     * Verifica a configuração de execução de um container: usuário, filesystem somente
     * leitura e capabilities removidas. Campos nulos não são verificados. A descrição
     * deriva dos campos presentes.
     */
    record ContainerConfiguracao(
            String nome,
            String usuario,
            Boolean somenteLeitura,
            java.util.List<String> capabilitiesRemovidas) implements Assercao {

        @Override
        public String descricao() {
            java.util.List<String> partes = new java.util.ArrayList<>();
            if (usuario != null) {
                partes.add("roda como `" + usuario + "`");
            }
            if (somenteLeitura != null && somenteLeitura) {
                partes.add("com filesystem somente leitura");
            }
            if (capabilitiesRemovidas != null && !capabilitiesRemovidas.isEmpty()) {
                partes.add("sem as capabilities " + capabilitiesRemovidas);
            }
            return "o container `" + nome + "` " + String.join(", ", partes);
        }
    }

    /** A imagem está publicada no registry referenciado. A descrição vem do Cenário. */
    record ImagemNoRegistry(String referencia, String descricao) implements Assercao {
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

    /**
     * Um endereço está no state do Terraform e, quando {@code atributo} vem preenchido,
     * com o valor esperado. Prova que o recurso nasceu do código, e não de um comando
     * digitado à mão.
     *
     * <p>O {@code diretorio} é relativo ao diretório de trabalho e chega injetado pelo
     * frontmatter do Cenário; o {@link MotorDeVerificacao} resolve e confina o caminho.
     * O YAML do Cenário não escolhe onde o Terraform roda.
     */
    record TerraformEstado(
            String diretorio,
            String endereco,
            String atributo,
            String esperado,
            String descricao) implements Assercao {

        public TerraformEstado {
            if ((atributo == null) != (esperado == null)) {
                throw new IllegalArgumentException(
                        "atributo e esperado devem ser informados juntos em terraform_estado");
            }
        }
    }

    /**
     * O `plan` não encontra nenhuma mudança pendente. É a Asserção que distingue uma
     * infraestrutura descrita por código de uma infraestrutura construída à mão: prova
     * idempotência e ausência de drift numa afirmação só.
     */
    record TerraformPlanoLimpo(String diretorio, String descricao) implements Assercao {
    }

    /**
     * Um serviço systemd está ativo e habilitado dentro do container Linux do Cenário.
     * Compara códigos de saída — 0 é o único positivo — porque `is-active` de uma unit
     * parada imprime `inactive`, e uma checagem por substring aprovaria o serviço parado.
     */
    record ServicoSystemd(
            String container,
            String nome,
            Boolean ativo,
            Boolean habilitado,
            String descricao) implements Assercao {

        public ServicoSystemd {
            if (ativo == null && habilitado == null) {
                throw new IllegalArgumentException(
                        "ativo ou habilitado deve ser informado em servico_systemd");
            }
        }
    }

    /**
     * Um arquivo ou diretório dentro do container Linux tem o modo, o dono e o grupo
     * esperados. Campos nulos não são verificados. A descrição vem do Cenário.
     */
    record ArquivoLinux(
            String container,
            String caminho,
            String modo,
            String dono,
            String grupo,
            String descricao) implements Assercao {
    }
}
