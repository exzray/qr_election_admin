package sample.controller;

import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import sample.model.Student;
import sample.utility.ControllerExtended;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StudentFormController extends ControllerExtended implements Initializable {

    // component
    private QueryDocumentSnapshot snapshot;
    private ToggleGroup toggle;
    private ListenerRegistration findRegistration;

    @FXML
    private JFXTextField text_matrik, text_email, text_name, text_course, text_image;
    @FXML
    private JFXPasswordField text_password;
    @FXML
    private JFXButton button_submit;
    @FXML
    private Button button_search;
    @FXML
    private RadioButton radio_new, radio_edit;

    // firebase
    private Firestore firestore;


    static FXMLLoader getInstance() {
        return new FXMLLoader(DashboardController.class.getResource("../layout/student_form.fxml"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComponent();
        initFirebase();

        setButton_search();
        setButton_submit();
        setGroupRadioButton();
    }

    private void initComponent() {
        toggle = new ToggleGroup();
    }

    private void initFirebase() {
        firestore = FirestoreClient.getFirestore();
    }

    private void findStudent(String matrik_id) {
        button_search.setDisable(true);

        CollectionReference ref = firestore.collection("students");

        // avoid duplicate listener on student document
        if (findRegistration != null) findRegistration.remove();

        findRegistration = ref
                .whereEqualTo("matrik_id", matrik_id)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<QueryDocumentSnapshot> list = value.getDocuments();

                        if (list.size() == 1) {
                            snapshot = list.get(0);
                            Student student = snapshot.toObject(Student.class);

                            text_email.setText(student.getEmail());
                            text_name.setText(student.getName());
                            text_course.setText(student.getCourse());
                            text_image.setText(student.getImage());

                            enableMatrikSearch(false);

                            text_name.setDisable(false);
                            text_course.setDisable(false);
                            text_image.setDisable(false);
                        }
                    }
                });
    }

    private void createStudent(Student student, String email, String pass) {

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(pass);

        try {
            UserRecord record = FirebaseAuth.getInstance().createUser(request);

            firestore
                    .document("students/" + record.getUid())
                    .set(student);

            resetStudent();

        } catch (FirebaseAuthException e) {
            e.printStackTrace();
        }
    }

    private void updateStudent(Student student) {

        if (snapshot != null) {
            button_submit.setDisable(true);

            DocumentReference reference = snapshot.getReference();

            reference
                    .set(student)
                    .addListener(() -> button_submit.setDisable(false), command -> command.run());
        }
    }

    private void setButton_search() {

        button_search.setOnMouseClicked(event -> {
            String matrik_id = text_matrik.getText().trim();

            if (!matrik_id.isEmpty()) findStudent(matrik_id);
        });
    }

    private void setButton_submit() {

        button_submit.setOnMouseClicked(event -> {
            // student data
            String email = text_email.getText().trim();
            String pass = text_password.getText().trim();

            String matrik = text_matrik.getText().trim();
            String name = text_name.getText().trim();
            String course = text_course.getText().trim();
            String image = text_image.getText().trim();

            // store student object
            Student student;

            if (snapshot != null) student = snapshot.toObject(Student.class);
            else student = new Student();

            student.setMatrik_id(matrik);
            student.setName(name);
            student.setEmail(email);
            student.setCourse(course);
            student.setImage(image);

            if (snapshot != null) updateStudent(student);
            else createStudent(student, email, pass);
        });
    }

    private void enableMatrikSearch(boolean state) {
        text_matrik.setDisable(!state);
        button_search.setDisable(!state);
    }

    private void enableDetailEdited(boolean state) {
        button_search.setDisable(state);

        text_email.setDisable(!state);
        text_password.setDisable(!state);
        text_name.setDisable(!state);
        text_course.setDisable(!state);
        text_image.setDisable(!state);
        text_matrik.setDisable(!state);
    }

    private void setGroupRadioButton() {
        toggle.getToggles().addAll(radio_edit, radio_new);
        toggle.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {

            RadioButton radio = (RadioButton) toggle.getSelectedToggle();

            if (radio == radio_new) {
                enableDetailEdited(true);

                resetStudent();

            } else {
                enableDetailEdited(false);
                enableMatrikSearch(true);
            }
        });

        radio_new.setSelected(true);
    }

    private void resetStudent() {
        snapshot = null;

        text_matrik.clear();
        text_name.clear();
        text_email.clear();
        text_password.clear();
        text_course.clear();
        text_image.clear();
    }
}
