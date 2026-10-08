package synapseforge.crud.DTO.Evento;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Cadastro ou edição de evento da agenda")
public class EventoRequestDTO {

    @NotBlank(message = "O ID do usuário é obrigatório")
    @Schema(description = "ID do usuário dono do evento", example = "6704a1c2e4b0f81a2c3d4e05")
    private String userId;

    @NotBlank(message = "O nome do evento é obrigatório")
    @Schema(description = "Título do evento", example = "Entrega do Dragão Vermelho")
    private String nome;

    @NotNull(message = "A data do evento é obrigatória")
    @Schema(description = "Data do evento (yyyy-MM-dd)", example = "2026-10-20")
    private String data;

    @Schema(description = "Detalhes do evento", example = "Cliente retira a peça na oficina.")
    private String descricao;

    @Schema(description = "Horário de início (HH:mm)", example = "14:00")
    private String horarioInicio;

    @Schema(description = "Horário de término (HH:mm)", example = "14:30")
    private String horarioFim;

    @ArraySchema(arraySchema = @Schema(description = "IDs dos usuários participantes"), schema = @Schema(example = "6704a1c2e4b0f81a2c3d4e02"))
    private List<String> participantes;

    public EventoRequestDTO() {}

    public EventoRequestDTO(String userId, String nome, String data, String descricao, String horarioInicio, String horarioFim, List<String> participantes) {
        this.userId = userId;
        this.nome = nome;
        this.data = data;
        this.descricao = descricao;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
        this.participantes = participantes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(String horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public String getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(String horarioFim) {
        this.horarioFim = horarioFim;
    }

    public List<String> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<String> participantes) {
        this.participantes = participantes;
    }
}
