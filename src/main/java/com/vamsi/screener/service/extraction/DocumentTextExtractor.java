package com.vamsi.screener.service.extraction;

import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.exception.DocumentParsingException;
import com.vamsi.screener.util.TextUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded file into clean plain text.
 *
 * <p>PDFs are read with Apache PDFBox; {@code .txt} and {@code .md} files are read as UTF-8. The
 * extractor validates size and type up front so bad input fails fast with a helpful message.
 */
@Component
public class DocumentTextExtractor {

    /** A resume shorter than this almost certainly means the PDF held no selectable text. */
    private static final int MINIMUM_USEFUL_LENGTH = 60;

    private final AppProperties.Upload uploadConfig;

    public DocumentTextExtractor(AppProperties properties) {
        this.uploadConfig = properties.getUpload();
    }

    /**
     * Extracts normalised text from the given file bytes.
     *
     * @param fileName original file name, used to pick the parser
     * @param bytes    raw file content
     * @return normalised plain text
     * @throws DocumentParsingException when the file is unsupported, too large, or has no text
     */
    public String extract(String fileName, byte[] bytes) {
        String safeName = fileName == null || fileName.isBlank() ? "resume" : fileName;
        if (bytes == null || bytes.length == 0) {
            throw new DocumentParsingException("File '" + safeName + "' is empty.");
        }
        if (bytes.length > uploadConfig.getMaxFileSizeBytes()) {
            throw new DocumentParsingException("File '" + safeName + "' is larger than the "
                    + (uploadConfig.getMaxFileSizeBytes() / (1024 * 1024)) + " MB limit.");
        }

        String extension = extensionOf(safeName);
        if (!uploadConfig.getAllowedExtensions().contains(extension)) {
            throw new DocumentParsingException("Unsupported file type '." + extension + "' for '" + safeName
                    + "'. Allowed types: " + String.join(", ", uploadConfig.getAllowedExtensions()) + ".");
        }

        String rawText = "pdf".equals(extension)
                ? readPdf(safeName, bytes)
                : new String(bytes, StandardCharsets.UTF_8);

        String normalized = TextUtils.normalize(rawText);
        if (normalized.length() < MINIMUM_USEFUL_LENGTH) {
            throw new DocumentParsingException("No readable text found in '" + safeName
                    + "'. Scanned or image-only PDFs need OCR before they can be screened.");
        }
        return normalized;
    }

    private String readPdf(String fileName, byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            stripper.setParagraphEnd("\n");
            return stripper.getText(document);
        } catch (IOException e) {
            throw new DocumentParsingException(
                    "Could not read PDF '" + fileName + "': " + e.getMessage(), e);
        }
    }

    /** @return the lowercase extension of the file name, or an empty string when there is none. */
    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
