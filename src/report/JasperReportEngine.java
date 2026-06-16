package report;

import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.*;

public class JasperReportEngine implements ReportEngine{

    @Override
    public void generatePDF(
            String templatePath,
            List<?> data,
            Map<String, Object> parameters,
            String outputPath) throws Exception {
        JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
        exporter.setConfiguration(new SimplePdfExporterConfiguration());
        exporter.exportReport();

        System.out.println("Report saved to: " + outputPath);
    }
}