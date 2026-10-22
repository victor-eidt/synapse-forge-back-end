package synapseforge.crud.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import synapseforge.crud.DTO.Admin.AdminPedidoUpdateRequestDTO;
import synapseforge.crud.infrastructure.entity.Pedido;

/**
 * Copia os campos do DTO de edição do admin para o pedido existente.
 * O MapStruct gera a implementação (PedidoAdminMapperImpl) na compilação,
 * com um getter/setter por campo de mesmo nome.
 * <p>
 * nullValuePropertyMappingStrategy = IGNORE: campo null no DTO significa
 * "não alterar" (PATCH parcial), então o valor atual do pedido é mantido.
 * unmappedTargetPolicy = IGNORE: o pedido tem campos que o admin não edita
 * por aqui (id, equipeId, datas, arquivos...), e eles ficam como estão.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PedidoAdminMapper {

    void atualizar(AdminPedidoUpdateRequestDTO dto, @MappingTarget Pedido pedido);
}
