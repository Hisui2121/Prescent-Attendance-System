package report;

import java.util.List;
import java.util.Map;

public interface ReportEngine {
    void generatePDF(
        String templatePath,
        List<?> data,
        Map<String, Object> parameters,
        String outputPath
    ) throws Exception;
}