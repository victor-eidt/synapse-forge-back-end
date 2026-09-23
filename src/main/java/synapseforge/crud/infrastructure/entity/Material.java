package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "materiais")
public class Material {

    @Id
    private String id;

    // Equipe (oficina) dona do registro; sai sempre do usuário logado, nunca do request
    private String equipeId;

    private String nome;
    private String tipo;
    private Double densidadeGcm3;
    private BigDecimal precoPorGrama;
    private Boolean ativo = true;

    private UnidadeMedida unidade = UnidadeMedida.G;

    // saldo e estoqueMinimo mantidos na unidade base de `unidade` (G, ML ou UN)
    private BigDecimal saldo = BigDecimal.ZERO;
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;
}
