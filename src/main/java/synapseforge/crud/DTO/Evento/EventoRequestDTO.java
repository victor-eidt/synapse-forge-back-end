package synapseforge.crud.DTO.Evento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class EventoRequestDTO {

    @NotBlank(message = "O ID do usuário é obrigatório")
    private String userId;

    @NotBlank(message = "O nome do evento é obrigatório")
    private String nome;

    @NotNull(message = "A data do evento é obrigatória")
    private String data;

    private String descricao;

    private String horarioInicio;

    private String horarioFim;

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
