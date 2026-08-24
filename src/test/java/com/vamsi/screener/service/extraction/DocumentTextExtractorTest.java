package com.vamsi.screener.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.exception.DocumentParsingException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentTextExtractorTest {

    private DocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DocumentTextExtractor(new AppProperties());
    }

    @Test
    @DisplayName("reads text out of a real PDF")
    void extractsTextFromPdf() throws Exception {
        byte[] pdf = pdfWithLines(List.of(
                "PRIYA SHARMA",
                "priya.sharma@example.com",
                "Backend engineer with 5 years of experience in Java and Spring Boot.",
                "Skills: Java, Spring Boot, PostgreSQL, Docker, Kubernetes"));

        String text = extractor.extract("priya.pdf", pdf);

        assertThat(text).contains("PRIYA SHARMA");
        assertThat(text).contains("priya.sharma@example.com");
        assertThat(text).contains("Spring Boot");
    }

    @Test
    @DisplayName("reads plain-text resumes as UTF-8")
    void extractsTextFromTxt() {
        String resume = "ANKIT VERMA\nankit@example.com\nJava developer with 4 years of experience.";

        String text = extractor.extract("ankit.txt", resume.getBytes(StandardCharsets.UTF_8));

        assertThat(text).contains("ANKIT VERMA").contains("Java developer");
    }

    @Test
    @DisplayName("rejects unsupported file types with a helpful message")
    void rejectsUnsupportedTypes() {
        byte[] content = "irrelevant but long enough to pass the length check".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract("resume.docx", content))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    @DisplayName("rejects empty files")
    void rejectsEmptyFiles() {
        assertThatThrownBy(() -> extractor.extract("resume.pdf", new byte[0]))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("is empty");
    }

    @Test
    @DisplayName("rejects documents with too little text, which usually means a scanned PDF")
    void rejectsTextlessDocuments() {
        assertThatThrownBy(() -> extractor.extract("scan.txt", "hi".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("No readable text");
    }

    @Test
    @DisplayName("file extensions are detected case-insensitively")
    void detectsExtensions() {
        assertThat(DocumentTextExtractor.extensionOf("Resume.PDF")).isEqualTo("pdf");
        assertThat(DocumentTextExtractor.extensionOf("resume")).isEmpty();
        assertThat(DocumentTextExtractor.extensionOf(null)).isEmpty();
    }

    private byte[] pdfWithLines(List<String> lines) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.setLeading(16f);
                content.newLineAtOffset(50, 780);
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
