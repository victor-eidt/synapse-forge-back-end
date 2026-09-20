package synapseforge.crud.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.util.Locale;

public final class ArquivoUtils {

    private ArquivoUtils() {
    }

    public static String contentType(GridFSFile file) {
        String metadataContentType = file.getMetadata() == null
                ? null
                : firstNonBlank(
                        file.getMetadata().getString("contentType"),
                        file.getMetadata().getString("_contentType")
                );

        String filename = file.getFilename();
        String extension = extension(filename);
        String inferredContentType = switch (extension) {
            case "obj" -> "model/obj";
            case "stl" -> "model/stl";
            case "3mf" -> "model/3mf";
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            default -> filename == null
                    ? null
                    : MediaTypeFactory.getMediaType(filename)
                            .map(MediaType::toString)
                            .orElse(null);
        };

        // O metadata e gravado no upload; o nome do arquivo vem do cliente, entao so
        // serve de fallback.
        if (metadataContentType != null) {
            return metadataContentType;
        }

        return inferredContentType == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : inferredContentType;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first
                : second != null && !second.isBlank() ? second
                : null;
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }

        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
