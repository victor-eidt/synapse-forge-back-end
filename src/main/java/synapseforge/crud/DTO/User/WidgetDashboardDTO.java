package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Widget do dashboard")
public class WidgetDashboardDTO {

    @Schema(description = "Identificador do widget", example = "pedidosPorMes",
            allowableValues = {"indicadores", "pedidosPorMes", "pedidosPorEtapa", "materiaisMaisUsados", "estoqueBaixo"})
    private String id;

    @Schema(description = "Se o widget aparece no dashboard", example = "true")
    private Boolean visivel;
}
