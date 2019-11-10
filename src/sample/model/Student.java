package sample.model;

public class Student {

    private String name = "";
    private String matrik_id = "";
    private String email = "";
    private String course = "";
    private String image = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMatrik_id() {
        return matrik_id;
    }

    public void setMatrik_id(String matrik_id) {
        this.matrik_id = matrik_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return getName();
    }
}
