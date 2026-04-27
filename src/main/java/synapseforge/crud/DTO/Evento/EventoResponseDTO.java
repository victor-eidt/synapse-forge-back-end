package synapseforge.crud.DTO.Evento;

import java.util.List;

public class EventoResponseDTO {

    private String id;
    private String userId;
    private String nome;
    private String data;
    private String descricao;
    private String horarioInicio;
    private String horarioFim;
    private List<String> participantes;

    public EventoResponseDTO() {}

    public EventoResponseDTO(String id, String userId, String nome, String data, String descricao, String horarioInicio, String horarioFim, List<String> participantes) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.data = data;
        this.descricao = descricao;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
        this.participantes = participantes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
