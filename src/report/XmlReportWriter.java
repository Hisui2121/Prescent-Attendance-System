package report;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class XmlReportWriter {

    public static void writeAttendanceXml(
            List<Object> records,
            Map<String, Object> params,
            String xmlOutputPath) throws Exception {

        try (PrintWriter pw = new PrintWriter(new FileWriter(xmlOutputPath))) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<attendanceReport>");
            pw.println("  <header>");
            pw.println("    <subjectName>" + escape(str(params.get("subjectName"))) + "</subjectName>");
            pw.println("    <sessionDate>" + escape(str(params.get("sessionDate"))) + "</sessionDate>");
            pw.println("    <professor>"   + escape(str(params.get("professor")))   + "</professor>");
            pw.println("    <classroom>"   + escape(str(params.get("classroom")))   + "</classroom>");
            pw.println("    <schedule>"    + escape(str(params.get("schedule")))    + "</schedule>");
            pw.println("  </header>");
            pw.println("  <records>");
            for (Object obj : records) {
                model.AttendanceRecord r = (model.AttendanceRecord) obj;
                pw.println("    <record>");
                pw.println("      <studentId>" + escape(r.getStudentId()) + "</studentId>");
                pw.println("      <fullName>"  + escape(r.getFullName())  + "</fullName>");
                pw.println("      <status>"    + escape(r.getStatus())    + "</status>");
                pw.println("    </record>");
            }
            pw.println("  </records>");
            pw.println("</attendanceReport>");
        }
    }

    public static void writeStudentXml(
            List<Object> students,
            String xmlOutputPath) throws Exception {

        try (PrintWriter pw = new PrintWriter(new FileWriter(xmlOutputPath))) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<studentReport>");
            pw.println("  <students>");
            for (Object obj : students) {
                model.Student s = (model.Student) obj;
                pw.println("    <student>");
                pw.println("      <studentId>" + escape(s.getStudentId()) + "</studentId>");
                pw.println("      <fullName>"  + escape(s.getFullName())  + "</fullName>");
                pw.println("      <course>"    + escape(s.getCourse())    + "</course>");
                pw.println("      <yearLevel>" + escape(s.getYearLevel()) + "</yearLevel>");
                pw.println("      <email>"     + escape(s.getEmail())     + "</email>");
                pw.println("    </student>");
            }
            pw.println("  </students>");
            pw.println("</studentReport>");
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}