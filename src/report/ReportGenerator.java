package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Student;
import model.AttendanceRecord;

public class ReportGenerator {

    private final ReportEngine engine;
    private static final String XML_TEMP_DIR = "reports/temp/";

    public ReportGenerator(ReportEngine engine) {
        this.engine = engine;
        new java.io.File(XML_TEMP_DIR).mkdirs();
    }

    public void generateAttendancePDF(
            List<?> data,
            String outputPath,
            String subjectName,
            String sessionDate,
            String professor,
            String classroom,
            String schedule) throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("subjectName", subjectName);
        params.put("sessionDate", sessionDate);
        params.put("professor",   professor);
        params.put("classroom",   classroom);
        params.put("schedule",    schedule);

        List<Object> records = new ArrayList<>(data);

        String xmlPath = XML_TEMP_DIR + "attendance_data.xml";
        XmlReportWriter.writeAttendanceXml(records, params, xmlPath);

        engine.generateAttendancePDF(xmlPath, outputPath);
    }

    public void generateStudentPDF(
            ArrayList<Student> students,
            String outputPath) throws Exception {

        List<Object> studentList = new ArrayList<>(students);

        String xmlPath = XML_TEMP_DIR + "student_data.xml";
        XmlReportWriter.writeStudentXml(studentList, xmlPath);

        engine.generateStudentPDF(xmlPath, outputPath);
    }
}