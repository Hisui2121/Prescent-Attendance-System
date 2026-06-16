package report;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import model.Student;

public class ReportGenerator {
	
	private final ReportEngine engine;
	
	public ReportGenerator(ReportEngine engine) {
		this.engine = engine;
	}
	
	public void generateStudentPDF(
    		ArrayList<Student> students, 
    		String outputPath) throws Exception {
    	engine.generatePDF("reports/student_list_report.xml", students, new HashMap<>(), outputPath);
    }

    // --- Backward-compatible static wrappers ---
    public static void generateAttendancePDF(
            List<?> data,
            String outputPath,
            String subjectName,
            String sessionDate,
            String professor,
            String classroom,
            String schedule) throws Exception {
        ReportEngine engine = new JasperReportEngine();
        Map<String, Object> params = new HashMap<>();
        params.put("subjectName", subjectName);
        params.put("sessionDate", sessionDate);
        params.put("professor", professor);
        params.put("classroom", classroom);
        params.put("schedule", schedule);
        engine.generatePDF("reports/attendance_report.xml", data, params, outputPath);
    }

    public static void generateStudentPDF(
            ArrayList<Student> students,
            String outputPath,
            String classTitle) throws Exception {
        // pass classTitle as a parameter to the report template
        ReportEngine engine = new JasperReportEngine();
        Map<String, Object> params = new HashMap<>();
        params.put("classTitle", classTitle == null ? "" : classTitle);
        // use the engine directly so we can pass parameters
        engine.generatePDF("reports/student_list_report.xml", students, params, outputPath);
    }
}