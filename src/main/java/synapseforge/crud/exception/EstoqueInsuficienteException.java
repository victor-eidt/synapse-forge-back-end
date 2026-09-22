package synapseforge.crud.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class EstoqueInsuficienteException extends RuntimeException {

    @Getter
    private final List<Falta> faltas;

    public EstoqueInsuficienteException(List<Falta> faltas) {
        super(montarMensagem(faltas));
        this.faltas = faltas;
    }

    private static String montarMensagem(List<Falta> faltas) {
        return faltas.stream()
                .map(f -> f.getNomeInsumo()
                        + " (necessário " + f.getNecessario().stripTrailingZeros().toPlainString()
                        + ", disponível " + f.getDisponivel().stripTrailingZeros().toPlainString() + ")")
                .collect(Collectors.joining("; ", "Estoque insuficiente: ", ""));
    }

    @Getter
    @AllArgsConstructor
    public static class Falta {
        private final String nomeInsumo;
        private final BigDecimal necessario;
        private final BigDecimal disponivel;
    }
}
