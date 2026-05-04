package synapseforge.crud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Evento.EventoRequestDTO;
import synapseforge.crud.DTO.Evento.EventoResponseDTO;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.infrastructure.repository.EventoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class EventoService {

    private static final Logger logger = LoggerFactory.getLogger(EventoService.class);

    @Autowired
    private EventoRepository repository;

    public Evento toEntity(EventoRequestDTO dto) {
        try {
            logger.info("Convertendo EventoRequestDTO para Evento: {}", dto);
            Evento evento = new Evento();

            evento.setUserId(dto.getUserId());
            evento.setNome(dto.getNome());
            evento.setData(dto.getData());
            evento.setDescricao(dto.getDescricao());
            evento.setHorarioInicio(dto.getHorarioInicio());
            evento.setHorarioFim(dto.getHorarioFim());
            evento.setParticipantes(dto.getParticipantes());

            logger.info("Conversão realizada com sucesso: {}", evento);
            return evento;
        } catch (Exception e) {
            logger.error("Erro ao converter EventoRequestDTO para Evento: {}", dto, e);
            throw e;
        }
    }

    public EventoResponseDTO toResponseDTO(Evento evento) {
        try {
            logger.info("Convertendo Evento para EventoResponseDTO: {}", evento);
            EventoResponseDTO response = new EventoResponseDTO(
                    evento.getId(),
                    evento.getUserId(),
                    evento.getNome(),
                    evento.getData(),
                    evento.getDescricao(),
                    evento.getHorarioInicio(),
                    evento.getHorarioFim(),
                    evento.getParticipantes()
            );
            logger.info("Conversão realizada com sucesso: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Erro ao converter Evento para EventoResponseDTO: {}", evento, e);
            throw e;
        }
    }

    public Evento criar(Evento evento) {
        try {
            logger.info("Criando novo evento: {}", evento);
            // regra de negócio
            // Timestamps removidos pois a entidade não os possui mais

            Evento saved = repository.save(evento);
            logger.info("Evento criado com sucesso: {}", saved);
            return saved;
        } catch (Exception e) {
            logger.error("Erro ao criar evento: {}", evento, e);
            throw e;
        }
    }

    public List<Evento> listar() {
        try {
            logger.info("Listando todos os eventos");
            List<Evento> eventos = repository.findAll();
            logger.info("Eventos listados com sucesso, total: {}", eventos.size());
            return eventos;
        } catch (Exception e) {
            logger.error("Erro ao listar eventos", e);
            throw e;
        }
    }

    public Optional<Evento> buscarPorId(String id) {
        try {
            logger.info("Buscando evento por ID: {}", id);
            Optional<Evento> evento = repository.findById(id);
            logger.info("Busca por ID realizada: {}", evento.isPresent() ? "Encontrado" : "Não encontrado");
            return evento;
        } catch (Exception e) {
            logger.error("Erro ao buscar evento por ID: {}", id, e);
            throw e;
        }
    }

    public List<Evento> buscarPorUserId(String userId) {
        try {
            logger.info("Buscando eventos por userId: {}", userId);
            List<Evento> eventos = repository.findByUserId(userId);
            logger.info("Eventos encontrados para userId {}, total: {}", userId, eventos.size());
            return eventos;
        } catch (Exception e) {
            logger.error("Erro ao buscar eventos por userId: {}", userId, e);
            throw e;
        }
    }

    public List<Evento> buscarPorUserIdAndMesAno(String userId, String mes, String ano) {
        try {
            logger.info("Buscando eventos por userId ou participante: {}, mês: {}, ano: {}", userId, mes, ano);
            // Dependendo do formato da data salva no banco, o pattern precisa ser ajustado.
            // Exemplo assumindo formato "yyyy-MM-dd...":
            String mesAnoPattern = String.format("^%s-%s-.*", ano, mes);
            List<Evento> eventos = repository.findByUserIdOrParticipanteAndMesAno(userId, mesAnoPattern);
            logger.info("Eventos encontrados para userId/participante {} no mês/ano {}/{}, total: {}", userId, mes, ano, eventos.size());
            return eventos;
        } catch (Exception e) {
            logger.error("Erro ao buscar eventos por userId/participante e mes/ano: {}, {}/{}", userId, mes, ano, e);
            throw e;
        }
    }

    public Evento atualizar(String id, Evento eventoAtualizado) {
        try {
            logger.info("Atualizando evento com ID: {}, dados: {}", id, eventoAtualizado);
            Evento evento = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

            evento.setNome(eventoAtualizado.getNome());
            evento.setData(eventoAtualizado.getData());
            evento.setDescricao(eventoAtualizado.getDescricao());
            evento.setHorarioInicio(eventoAtualizado.getHorarioInicio());
            evento.setHorarioFim(eventoAtualizado.getHorarioFim());
            evento.setParticipantes(eventoAtualizado.getParticipantes());
            // Timestamp removido

            Evento updated = repository.save(evento);
            logger.info("Evento atualizado com sucesso: {}", updated);
            return updated;
        } catch (Exception e) {
            logger.error("Erro ao atualizar evento com ID: {}", id, e);
            throw e;
        }
    }

    public void deletar(String id) {
        try {
            logger.info("Deletando evento com ID: {}", id);
            repository.deleteById(id);
            logger.info("Evento deletado com sucesso, ID: {}", id);
        } catch (Exception e) {
            logger.error("Erro ao deletar evento com ID: {}", id, e);
            throw e;
        }
    }

    public List<Evento> criarVarios(List<Evento> eventos) {
        try {
            logger.info("Criando múltiplos eventos, total: {}", eventos.size());
            // Timestamps removidos
            // eventos.forEach(evento -> {
            //     evento.setCriadoEm(LocalDateTime.now());
            //     evento.setAtualizadoEm(LocalDateTime.now());
            // });

            List<Evento> saved = repository.saveAll(eventos);
            logger.info("Múltiplos eventos criados com sucesso, total: {}", saved.size());
            return saved;
        } catch (Exception e) {
            logger.error("Erro ao criar múltiplos eventos", e);
            throw e;
        }
    }
}
