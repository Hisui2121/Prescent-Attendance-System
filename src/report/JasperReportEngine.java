package report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.*;

import java.util.HashMap;

public class JasperReportEngine implements ReportEngine {

    private static final String STUDENT_TEMPLATE    = "reports/student_list_report.xml";
    private static final String ATTENDANCE_TEMPLATE = "reports/attendance_report.xml";

    @Override
    public void generateAttendancePDF(
            String xmlDataPath,
            String outputPath) throws Exception {

        JasperReport jasperReport = JasperCompileManager.compileReport(ATTENDANCE_TEMPLATE);

        JRXmlDataSource headerSource = new JRXmlDataSource(xmlDataPath, "/attendanceReport/header");
        HashMap<String, Object> params = new HashMap<>();
        if (headerSource.next()) {
            params.put("subjectName", getXmlValue(xmlDataPath, "/attendanceReport/header/subjectName"));
            params.put("sessionDate", getXmlValue(xmlDataPath, "/attendanceReport/header/sessionDate"));
            params.put("professor",   getXmlValue(xmlDataPath, "/attendanceReport/header/professor"));
            params.put("classroom",   getXmlValue(xmlDataPath, "/attendanceReport/header/classroom"));
            params.put("schedule",    getXmlValue(xmlDataPath, "/attendanceReport/header/schedule"));
        }

        JRXmlDataSource dataSource = new JRXmlDataSource(xmlDataPath, "/attendanceReport/records/record");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
        exporter.setConfiguration(new SimplePdfExporterConfiguration());
        exporter.exportReport();

        System.out.println("Report saved to: " + outputPath);
    }

    @Override
    public void generateStudentPDF(
            String xmlDataPath,
            String outputPath) throws Exception {

        JasperReport jasperReport = JasperCompileManager.compileReport(STUDENT_TEMPLATE);

        JRXmlDataSource dataSource = new JRXmlDataSource(xmlDataPath, "/studentReport/students/student");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), dataSource);

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
        exporter.setConfiguration(new SimplePdfExporterConfiguration());
        exporter.exportReport();

        System.out.println("Report saved to: " + outputPath);
    }

    private String getXmlValue(String xmlPath, String xpath) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory =
                javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.File(xmlPath));
            javax.xml.xpath.XPathFactory xpf = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xp = xpf.newXPath();
            return xp.evaluate(xpath, doc);
        } catch (Exception e) {
            return "";
        }
    }
}