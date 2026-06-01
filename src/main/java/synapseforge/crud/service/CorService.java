package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Cor.CorRequestDTO;
import synapseforge.crud.DTO.Cor.CorResponseDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.repository.CorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CorService {

    @Autowired
    private CorRepository repository;

    public Cor toEntity(CorRequestDTO dto, String usuarioId) {
        Cor cor = new Cor();
        cor.setUsuarioId(usuarioId);
        cor.setNome(dto.getNome());
        cor.setFornecedor(dto.getFornecedor());
        cor.setCodigo(dto.getCodigo());
        cor.setHex(dto.getHex());
        cor.setAcabamento(dto.getAcabamento());
        cor.setEstoqueMl(dto.getEstoqueMl());
        cor.setEstoqueMinimoMl(dto.getEstoqueMinimoMl());
        cor.setCustoMl(dto.getCustoMl());
        return cor;
    }

    public CorResponseDTO toResponseDTO(Cor cor) {
        return new CorResponseDTO(
                cor.getId(),
                cor.getNome(),
                cor.getFornecedor(),
                cor.getCodigo(),
                cor.getHex(),
                cor.getAcabamento(),
                cor.getEstoqueMl(),
                cor.getEstoqueMinimoMl(),
                cor.getCustoMl(),
                cor.getCriadoEm(),
                cor.getAtualizadoEm()
        );
    }

    public Cor criar(Cor cor) {
        cor.setCriadoEm(LocalDateTime.now());
        cor.setAtualizadoEm(LocalDateTime.now());
        return repository.save(cor);
    }

    public List<Cor> listar(String usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    public Optional<Cor> buscarPorId(String id, String usuarioId) {
        return repository.findById(id)
                .filter(c -> usuarioId.equals(c.getUsuarioId()));
    }

    public Cor atualizar(String id, String usuarioId, Cor dados) {
        Cor cor = repository.findById(id)
                .filter(c -> usuarioId.equals(c.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Cor não encontrada"));
        cor.setNome(dados.getNome());
        cor.setFornecedor(dados.getFornecedor());
        cor.setCodigo(dados.getCodigo());
        cor.setHex(dados.getHex());
        cor.setAcabamento(dados.getAcabamento());
        cor.setEstoqueMl(dados.getEstoqueMl());
        cor.setEstoqueMinimoMl(dados.getEstoqueMinimoMl());
        cor.setCustoMl(dados.getCustoMl());
        cor.setAtualizadoEm(LocalDateTime.now());
        return repository.save(cor);
    }

    public void deletar(String id, String usuarioId) {
        repository.findById(id)
                .filter(c -> usuarioId.equals(c.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Cor não encontrada"));
        repository.deleteById(id);
    }
}
