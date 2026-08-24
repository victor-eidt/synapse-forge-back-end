package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.repository.MaterialRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository repository;

    public MaterialResponseDTO criar(MaterialRequestDTO dto) {
        Material material = new Material();
        aplicarDados(material, dto);
        return toResponseDTO(repository.save(material));
    }

    public List<MaterialResponseDTO> listarAtivos() {
        return repository.findByAtivoTrue().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public MaterialResponseDTO buscarPorId(String id) {
        return toResponseDTO(buscarEntidade(id));
    }

    public MaterialResponseDTO atualizar(String id, MaterialRequestDTO dto) {
        Material material = buscarEntidade(id);
        aplicarDados(material, dto);
        return toResponseDTO(repository.save(material));
    }

    public void inativar(String id) {
        Material material = buscarEntidade(id);
        material.setAtivo(false);
        repository.save(material);
    }

    private Material buscarEntidade(String id) {
        return repository.findById(id)
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
        if (dto.getSaldo() != null) {
            material.setSaldo(dto.getSaldo());
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
