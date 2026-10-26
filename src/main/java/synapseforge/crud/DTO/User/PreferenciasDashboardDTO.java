package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Período padrão dos filtros e widgets do dashboard")
public class PreferenciasDashboardDTO {

    @Schema(description = "Período padrão dos filtros do dashboard", example = "90d",
            allowableValues = {"30d", "90d", "6m", "12m", "ano", "tudo"})
    private String periodo;

    @ArraySchema(
            arraySchema = @Schema(description = "Widgets na ordem de exibição (sem id repetido)"),
            minItems = 1,
            maxItems = 10
    )
    private List<WidgetDashboardDTO> widgets;
}
