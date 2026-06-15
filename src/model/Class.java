package model;

public class Class {

    private int id;
    private String classCode;
    private String className;
    private String teacherId;
    private String schedule;
    private String room;

    public Class() {

    }

    public Class(String classCode,
                 String className,
                 String teacherId,
                 String schedule,
                 String room) {

        this.classCode = classCode;
        this.className = className;
        this.teacherId = teacherId;
        this.schedule = schedule;
        this.room = room;
    }

    public Class(int id,
                 String classCode,
                 String className,
                 String teacherId,
                 String schedule,
                 String room) {

        this.id = id;
        this.classCode = classCode;
        this.className = className;
        this.teacherId = teacherId;
        this.schedule = schedule;
        this.room = room;
    }

    // GETTERS & SETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    @Override
    public String toString() {
        if (classCode != null && className != null) {
            return classCode + " - " + className;
        }
        return classCode != null ? classCode : "";
    }
}