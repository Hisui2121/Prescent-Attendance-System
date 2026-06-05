package report;

public interface ReportEngine {
    void generateAttendancePDF(
    	String xmlDataPath, 
    	String outputPath
    ) throws Exception;
    void generateStudentPDF(
    	String xmlDataPath, 
    	String outputPath
    ) throws Exception;
}