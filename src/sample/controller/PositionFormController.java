package sample.controller;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import sample.model.Position;
import sample.model.Student;
import sample.model.Vote;
import sample.utility.ControllerExtended;

import java.net.URL;
import java.util.*;

public class PositionFormController extends ControllerExtended implements Initializable {

    private QueryDocumentSnapshot campaign_snapshot;
    private QueryDocumentSnapshot position_snapshot;
    private ListenerRegistration studentRegistration;
    private ObservableList<Vote> votes = FXCollections.observableArrayList();
    private ObservableList<QueryDocumentSnapshot> students = FXCollections.observableArrayList();
    private ObservableList<QueryDocumentSnapshot> positions = FXCollections.observableArrayList();

    @FXML
    private ListView<QueryDocumentSnapshot> list_position;

    @FXML
    private VBox container_2;

    @FXML
    private HBox container_3;

    @FXML
    private Button button_clear, button_delete, button_submit, button_candidate, button_delete_candidate;

    @FXML
    private JFXTextField field_title, field_arrangement;

    @FXML
    private ComboBox<QueryDocumentSnapshot> combo_student;

    @FXML
    private TableView<Vote> table_result;
    @FXML
    private TableColumn<Vote, String> column_uid, column_name, column_vote;

    // firebase
    private Firestore firestore = FirestoreClient.getFirestore();

    public static FXMLLoader getInstance() {
        return new FXMLLoader(DashboardController.class.getResource("../layout/position_form.fxml"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        list_position.setItems(positions);
        list_position.setCellFactory(new PositionCallback());
        list_position.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        BooleanBinding binding_selected = list_position.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding binding_empty = Bindings
                .isEmpty(field_title.textProperty())
                .or(Bindings.isEmpty(field_arrangement.textProperty()));

        container_2.disableProperty().bind(binding_selected);
        container_3.disableProperty().bind(binding_selected);
        button_submit.disableProperty().bind(binding_empty);

        button_clear.setOnAction(event -> {
            list_position.getSelectionModel().clearSelection();

            resetData();
        });
        button_delete.setOnAction(event -> {
            if (position_snapshot != null) {
                position_snapshot.getReference().delete();
            }
        });
        button_submit.setOnAction(event -> {
            Position position;

            if (position_snapshot != null) position = position_snapshot.toObject(Position.class);
            else position = new Position();

            String str_title = field_title.getText().trim();
            Integer int_arrangement = Integer.parseInt(field_arrangement.getText());

            position.setTitle(str_title);
            position.setArrangement(int_arrangement);

            if (position_snapshot != null) {
                position_snapshot
                        .getReference()
                        .set(position);
            } else {
                campaign_snapshot
                        .getReference()
                        .collection("positions")
                        .add(position);
            }

            resetData();
        });

        StudentCallback callback = new StudentCallback();
        combo_student.setItems(students);
        combo_student.setButtonCell(callback.call(null));
        combo_student.setCellFactory(callback);

        table_result.setItems(votes);
        table_result.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        column_uid.setCellValueFactory(new ResultCallback(ResultCallback.CELL.UID));
        column_name.setCellValueFactory(new ResultCallback(ResultCallback.CELL.NAME));
        column_vote.setCellValueFactory(new ResultCallback(ResultCallback.CELL.COUNT));

        BooleanBinding binding = table_result.getSelectionModel().selectedItemProperty().isNull();

        button_delete_candidate.disableProperty().bind(binding);
        button_delete_candidate.setOnMouseClicked(event -> {
            Vote vote = table_result.getSelectionModel().getSelectedItem();

            if (vote != null) {
                Position position = position_snapshot.toObject(Position.class);

                position.getCandidates().remove(vote.getUid());

                List<String> keys = new ArrayList<>(position.getVotes().keySet());

                for (String key : keys) {

                    if (position.getVotes().get(key).equals(vote.getUid())) {
                        synchronized (this) {
                            position.getVotes().remove(key);
                        }
                    }
                }

                position_snapshot
                        .getReference()
                        .set(position);
            }
        });

        button_candidate.setOnMouseClicked(event -> {
            QueryDocumentSnapshot snapshot = combo_student.getSelectionModel().getSelectedItem();

            if (snapshot != null) {
                Position position = position_snapshot.toObject(Position.class);

                if (!position.getCandidates().contains(snapshot.getId()))
                    position.getCandidates().add(snapshot.getId());

                position_snapshot
                        .getReference()
                        .set(position);
            }
        });
    }

    private void resetData() {
        position_snapshot = null;

        field_title.clear();
        field_arrangement.clear();
    }

    public void setCampaign(QueryDocumentSnapshot snapshot) {
        campaign_snapshot = snapshot;

        listenToPosition();
    }

    private void listenToPosition() {
        campaign_snapshot
                .getReference()
                .collection("positions")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        Platform.runLater(() -> {
                            positions.clear();
                            positions.addAll(value.getDocuments());
                        });
                    }
                });
    }

    private void listenToStudent() {
        if (studentRegistration != null) studentRegistration.remove();

        studentRegistration = firestore
                .collection("students")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        Platform.runLater(() -> {
                            students.clear();
                            students.addAll(value.getDocuments());

                            if (position_snapshot != null) {
                                Position position = position_snapshot.toObject(Position.class);

                                calculateVote(position);
                            }
                        });
                    }
                });
    }

    private void calculateVote(Position position) {
        votes.clear();

        List<String> candidates = position.getCandidates();
        List<String> vote_data = new ArrayList<>(position.getVotes().values());
        Map<String, Vote> candidate_vote = new HashMap<>();

        for (String candidate : candidates) {
            Vote vote = new Vote(candidate);

            for (QueryDocumentSnapshot snapshot : students) {

                if (snapshot.getId().equals(candidate)) {
                    Student student = snapshot.toObject(Student.class);
                    vote.setName(student.getName());
                }
            }

            candidate_vote.put(candidate, vote);
        }

        for (String candidate : vote_data) {
            int count = candidate_vote.get(candidate).getCount();
            count++;

            candidate_vote.get(candidate).setCount(count);
        }

        votes.addAll(candidate_vote.values());
    }

    class PositionCallback implements Callback<ListView<QueryDocumentSnapshot>, ListCell<QueryDocumentSnapshot>> {

        @Override
        public ListCell<QueryDocumentSnapshot> call(ListView<QueryDocumentSnapshot> param) {
            return new PositionListCell();
        }

        class PositionListCell extends ListCell<QueryDocumentSnapshot> {

            @Override
            protected void updateItem(QueryDocumentSnapshot item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Position position = item.toObject(Position.class);

                    setText(position.getTitle());

                    setOnMouseClicked(new CellOnClick(item));
                }
            }
        }

        class CellOnClick implements EventHandler<MouseEvent> {

            private QueryDocumentSnapshot snapshot;

            public CellOnClick(QueryDocumentSnapshot snapshot) {
                this.snapshot = snapshot;
            }

            @Override
            public void handle(MouseEvent event) {
                position_snapshot = snapshot;

                Position position = position_snapshot.toObject(Position.class);

                field_title.setText(position.getTitle());
                field_arrangement.setText(String.valueOf(position.getArrangement()));

                listenToStudent();
            }
        }
    }

    class StudentCallback implements Callback<ListView<QueryDocumentSnapshot>, ListCell<QueryDocumentSnapshot>> {

        @Override
        public ListCell<QueryDocumentSnapshot> call(ListView<QueryDocumentSnapshot> param) {
            return new StudentListCell();
        }

        class StudentListCell extends ListCell<QueryDocumentSnapshot> {

            @Override
            protected void updateItem(QueryDocumentSnapshot item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Student student = item.toObject(Student.class);

                    setText(student.getName());
                }
            }
        }
    }

    private static class ResultCallback implements Callback<TableColumn.CellDataFeatures<Vote, String>, ObservableValue<String>> {

        private CELL cell;

        enum CELL {
            UID,
            NAME,
            COUNT
        }

        public ResultCallback(CELL cell) {
            this.cell = cell;
        }

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Vote, String> param) {
            Vote vote = param.getValue();
            SimpleStringProperty value = new SimpleStringProperty();

            switch (cell) {
                case NAME:
                    value.setValue(vote.getName());
                    break;
                case UID:
                    value.setValue(vote.getUid());
                    break;
                case COUNT:
                    value.setValue(String.valueOf(vote.getCount()));
                    break;
            }

            return value;
        }
    }
}
