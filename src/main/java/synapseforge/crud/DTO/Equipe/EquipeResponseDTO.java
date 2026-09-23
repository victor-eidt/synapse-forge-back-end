package synapseforge.crud.DTO.Equipe;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EquipeResponseDTO {

    private String id;
    private String nome;
    private String gerenteId;

    // Imagens convertidas para Base64
    private String fotoBase64;
    private String bannerBase64;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}