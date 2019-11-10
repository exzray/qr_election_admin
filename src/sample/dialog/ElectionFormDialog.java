package sample.dialog;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import sample.model.Election;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class ElectionFormDialog extends Dialog<Election> {

    // widget
    private VBox container;
    private JFXTextField field_title;
    private JFXDatePicker field_date;

    // data
    private QueryDocumentSnapshot snapshot;


    public ElectionFormDialog(QueryDocumentSnapshot data) {
        this.setTitle("Election Form");

        // init widget
        container = new VBox(10);
        field_title = new JFXTextField();
        field_date = new JFXDatePicker();
        this.snapshot = data;

        setupWidget();
    }

    private void setupWidget() {
        container.setPadding(new Insets(30));
        container.getChildren().addAll(field_title, field_date);

        field_title.setPromptText("Title");
        field_title.setPrefWidth(200);
        field_date.setPromptText("Date of Election");
        field_date.setPrefWidth(200);
        field_date.setValue(LocalDate.now());

        if (snapshot != null) {

            Election election = snapshot.toObject(Election.class);
            LocalDate date = election.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            field_title.setText(election.getTitle());
            field_date.setValue(date);
        }

        ButtonType button_positive = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);


        this.getDialogPane().getButtonTypes().addAll(button_positive);
        this.getDialogPane().setContent(container);
        this.setResultConverter(new Callback<ButtonType, Election>() {
            @Override
            public Election call(ButtonType param) {
                LocalDate localDate = field_date.getValue();

                String title = field_title.getText();
                Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                // store data
                Election election;

                if (snapshot != null) {
                    election = snapshot.toObject(Election.class);
                    election.setTitle(title);
                    election.setStart(date);

                } else {
                    election = new Election();
                    election.setTitle(title);
                    election.setStart(date);
                }

                return election;
            }
        });
    }
}
