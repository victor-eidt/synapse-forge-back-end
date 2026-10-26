package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDashboard {

    // Nome do campo explícito: sem o @Field o Spring Data trata "id" como identificador
    // e grava "_id" dentro de cada item da lista.
    @Field("id")
    private String id;

    private Boolean visivel;
}
