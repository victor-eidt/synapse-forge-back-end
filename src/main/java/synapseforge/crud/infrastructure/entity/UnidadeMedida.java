package synapseforge.crud.infrastructure.entity;

import java.math.BigDecimal;

public enum UnidadeMedida {

    G(null, BigDecimal.ONE),
    KG(G, new BigDecimal("1000")),
    ML(null, BigDecimal.ONE),
    L(ML, new BigDecimal("1000")),
    UN(null, BigDecimal.ONE);

    private final UnidadeMedida base;
    private final BigDecimal fatorParaBase;

    UnidadeMedida(UnidadeMedida base, BigDecimal fatorParaBase) {
        this.base = base;
        this.fatorParaBase = fatorParaBase;
    }

    public UnidadeMedida base() {
        return base == null ? this : base;
    }

    public BigDecimal paraBase(BigDecimal quantidade) {
        return quantidade.multiply(fatorParaBase);
    }
}
