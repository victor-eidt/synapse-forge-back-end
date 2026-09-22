package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.repository.MaterialRepository;

import java.math.BigDecimal;
import java.util.List;

// Materiais (e seus saldos) são da equipe: cada oficina tem o próprio estoque.
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository repository;
    private final EquipeContexto equipeContexto;

    public MaterialResponseDTO criar(MaterialRequestDTO dto, String usuarioId) {
        Material material = new Material();
        material.setEquipeId(equipeContexto.equipeObrigatoria(usuarioId));
        aplicarDados(material, dto);
        if (material.getUnidade() == null) {
            material.setUnidade(UnidadeMedida.G);
        }
        if (material.getEstoqueMinimo() == null) {
            material.setEstoqueMinimo(BigDecimal.ZERO);
        }
        // saldo sempre nasce zerado; entra no estoque apenas por movimentação
        material.setSaldo(BigDecimal.ZERO);
        return toResponseDTO(repository.save(material));
    }

    public List<MaterialResponseDTO> listarAtivos(String usuarioId) {
        return equipeContexto.equipeDe(usuarioId)
                .map(repository::findByEquipeIdAndAtivoTrue)
                .orElse(List.of())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public MaterialResponseDTO buscarPorId(String id, String usuarioId) {
        // leitura de um registro: sem equipe, simplesmente não existe
        Material material = equipeContexto.equipeDe(usuarioId)
                .flatMap(equipeId -> repository.findByIdAndEquipeId(id, equipeId))
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));
        return toResponseDTO(material);
    }

    public MaterialResponseDTO atualizar(String id, MaterialRequestDTO dto, String usuarioId) {
        Material material = buscarEntidade(id, usuarioId);
        aplicarDados(material, dto);
        return toResponseDTO(repository.save(material));
    }

    public void inativar(String id, String usuarioId) {
        Material material = buscarEntidade(id, usuarioId);
        material.setAtivo(false);
        repository.save(material);
    }

    // escrita: sem equipe -> 403 SEM_EQUIPE; material de outra equipe -> não encontrado
    private Material buscarEntidade(String id, String usuarioId) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        return repository.findByIdAndEquipeId(id, equipeId)
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));
    }

    private void aplicarDados(Material material, MaterialRequestDTO dto) {
        material.setNome(dto.getNome());
        material.setTipo(dto.getTipo());
        material.setDensidadeGcm3(dto.getDensidadeGcm3());
        material.setPrecoPorGrama(dto.getPrecoPorGrama());
        material.setAtivo(dto.getAtivo() == null ? Boolean.TRUE : dto.getAtivo());
        if (dto.getUnidade() != null) {
            material.setUnidade(dto.getUnidade());
        }
        if (dto.getEstoqueMinimo() != null) {
            material.setEstoqueMinimo(dto.getEstoqueMinimo());
        }
    }

    private MaterialResponseDTO toResponseDTO(Material material) {
        return new MaterialResponseDTO(
                material.getId(),
                material.getNome(),
                material.getTipo(),
                material.getDensidadeGcm3(),
                material.getPrecoPorGrama(),
                material.getAtivo(),
                material.getUnidade(),
                material.getSaldo(),
                material.getEstoqueMinimo()
        );
    }
}
