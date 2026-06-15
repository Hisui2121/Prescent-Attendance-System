package report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import java.util.*;
import model.Student;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;

public class ReportGenerator {

	public static void generateAttendancePDF(
	        List<?> data,
	        String outputPath,
	        String subjectName,
	        String sessionDate,
	        String professor,
	        String classroom,
	        String schedule) throws JRException {

	    JasperReport jasperReport = JasperCompileManager.compileReport(
	        "reports/attendance_report.jrxml"
	    );

	    JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

	    Map<String, Object> params = new HashMap<>();
	    params.put("subjectName", subjectName);
	    params.put("sessionDate", sessionDate);
	    params.put("professor",   professor);
	    params.put("classroom",   classroom);
	    params.put("schedule",    schedule);

	    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

	    JRPdfExporter exporter = new JRPdfExporter();
	    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
	    exporter.setConfiguration(new SimplePdfExporterConfiguration());
	    exporter.exportReport();

	    System.out.println("Report saved to: " + outputPath);
	}
    
    // Added classTitle parameter to show class code/name in the masterlist header
    public static void generateStudentPDF(ArrayList<Student> students, String outputPath, String classTitle) throws JRException {

        JasperReport jasperReport = JasperCompileManager.compileReport(
            "reports/student_list_report.jrxml"
        );

        JRBeanCollectionDataSource dataSource =
            new JRBeanCollectionDataSource(students);

        Map<String, Object> params = new HashMap<>();
        params.put("classTitle", classTitle == null ? "" : classTitle);

        JasperPrint jasperPrint = JasperFillManager.fillReport(
            jasperReport, params, dataSource
        );

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        exporter.setConfiguration(config);
        exporter.exportReport();

        System.out.println("Report saved to: " + outputPath);
    }
}