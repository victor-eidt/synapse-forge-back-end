package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Aviso persistido para um usuário, exibido no sino da Sidebar.
 * O texto é montado no front (i18n) a partir do tipo e do título,
 * por isso aqui só ficam os dados, não a frase pronta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notificacoes")
public class Notificacao {

    @Id
    private String id;

    // Destinatário do aviso
    @Indexed
    private String usuarioId;

    private TipoNotificacao tipo;

    // Registro de origem (ex.: id do pedido finalizado)
    private String referenciaId;

    // Texto principal exibido no sino (ex.: nome do projeto)
    private String titulo;

    // Dado extra que completa a frase no front (ex.: a etapa nova: "PINTURA").
    // Guardado como código, não texto pronto, para o front traduzir.
    private String detalhe;

    private boolean lida;

    private LocalDateTime criadaEm;

    private LocalDateTime lidaEm;
}
