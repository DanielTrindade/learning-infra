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
}
