package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Dashboard personalizado: período padrão dos filtros e widgets na ordem de exibição. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasDashboard {

    private String periodo;
    private List<WidgetDashboard> widgets;
}
