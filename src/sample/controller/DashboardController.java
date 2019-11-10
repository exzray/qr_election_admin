package sample.controller;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.util.Callback;
import sample.dialog.ElectionFormDialog;
import sample.model.Election;
import sample.utility.ControllerExtended;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController extends ControllerExtended implements Initializable {

    public static ListenerRegistration listener_election;

    // widget
    @FXML
    private Button button_student, button_election;

    // widget group of table election
    @FXML
    private TableView<QueryDocumentSnapshot> table_election;
    @FXML
    private TableColumn<QueryDocumentSnapshot, String> column_uid, column_name, column_date;

    // component
    private ObservableList<QueryDocumentSnapshot> list_election;

    // firebase
    private Firestore firestore;

    public static FXMLLoader getInstance() {
        return new FXMLLoader(DashboardController.class.getResource("../layout/dashboard.fxml"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComponent();
        initFirebase();

        setupWidget();

        getElectionList();
    }

    private void initComponent() {
        list_election = FXCollections.observableArrayList();
    }

    private void initFirebase() {
        firestore = FirestoreClient.getFirestore();
    }

    private void setupWidget() {
        button_election.setOnAction(new ActionButtonElection());
        button_student.setOnAction(new ActionButtonStudent());

        table_election.setItems(list_election);
        table_election.setRowFactory(new ElectionRowFactory());
        table_election.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        column_uid.setCellValueFactory(new ElectionCell(ElectionCell.CELL.UID));
        column_name.setCellValueFactory(new ElectionCell(ElectionCell.CELL.NAME));
        column_date.setCellValueFactory(new ElectionCell(ElectionCell.CELL.DATE));
    }

    private void getElectionList() {
        if (listener_election != null) listener_election.remove();

        CollectionReference reference = firestore.collection("elections");

        listener_election = reference.addSnapshotListener((value, error) -> {

            if (value != null) {

                List<QueryDocumentSnapshot> documents = value.getDocuments();

                // update list_election
                list_election.clear();
                list_election.addAll(documents);
            }
        });
    }

    class ElectionRowFactory implements Callback<TableView<QueryDocumentSnapshot>, TableRow<QueryDocumentSnapshot>> {

        @Override
        public TableRow<QueryDocumentSnapshot> call(TableView<QueryDocumentSnapshot> param) {

            final TableRow<QueryDocumentSnapshot> row = new TableRow<>();
            final ContextMenu menu = new ContextMenu();
            final MenuItem item_edit = new MenuItem("Edit Election");
            final MenuItem item_delete = new MenuItem("Delete Election");
            final MenuItem item_campaign = new MenuItem("View Campaign");
            final MenuItem item_qr = new MenuItem("Create QR Code");

            // add menu item into context menu
            menu.getItems().addAll(item_edit, item_delete, item_campaign, item_qr);

            // setup menu item action when clicked
            item_edit.setOnAction(new ItemEditAction(row));
            item_delete.setOnAction(new ItemDeleteAction(row));
            item_campaign.setOnAction(new ItemCampaignAction(row));
            item_qr.setOnAction(new ItemQR(row));

            // bind context menu to row
            row
                    .contextMenuProperty()
                    .bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));

            return row;
        }

        class ItemEditAction implements EventHandler<ActionEvent> {

            private final TableRow<QueryDocumentSnapshot> row;

            private ItemEditAction(TableRow<QueryDocumentSnapshot> row) {
                this.row = row;
            }

            @Override
            public void handle(ActionEvent event) {
                QueryDocumentSnapshot snapshot = row.getItem();

                ElectionFormDialog dialog = new ElectionFormDialog(snapshot);
                Optional<Election> optional = dialog.showAndWait();

                optional.ifPresent(election -> snapshot.getReference().set(election));
            }
        }

        class ItemDeleteAction implements EventHandler<ActionEvent> {

            private final TableRow<QueryDocumentSnapshot> row;

            private ItemDeleteAction(TableRow<QueryDocumentSnapshot> row) {
                this.row = row;
            }

            @Override
            public void handle(ActionEvent event) {
                QueryDocumentSnapshot snapshot = row.getItem();

                snapshot.getReference().delete();
            }
        }

        class ItemCampaignAction implements EventHandler<ActionEvent> {

            private final TableRow<QueryDocumentSnapshot> row;

            private ItemCampaignAction(TableRow<QueryDocumentSnapshot> row) {
                this.row = row;
            }

            @Override
            public void handle(ActionEvent event) {
                QueryDocumentSnapshot snapshot = row.getItem();
                Election election = snapshot.toObject(Election.class);

                FXMLLoader loader = CampaignController.getInstance();

                try {
                    Parent parent = loader.load();
                    CampaignController controller = loader.getController();

                    stage.setScene(new Scene(parent));
                    stage.setTitle(election.getTitle());

                    controller.setStage(stage);
                    controller.setElection(snapshot);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        class ItemQR implements EventHandler<ActionEvent> {

            private final TableRow<QueryDocumentSnapshot> row;

            private ItemQR(TableRow<QueryDocumentSnapshot> row) {
                this.row = row;
            }

            @Override
            public void handle(ActionEvent event) {
                QueryDocumentSnapshot snapshot = row.getItem();
                Election election = snapshot.toObject(Election.class);

                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        QRCodeWriter codeWriter = new QRCodeWriter();

                        try {
                            BitMatrix bitMatrix = codeWriter.encode(snapshot.getId(), BarcodeFormat.QR_CODE, 350, 350);
                            Path path = FileSystems.getDefault().getPath("./" + election.getTitle() + ".png");
                            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
                        } catch (WriterException | IOException e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                };

                Thread thread = new Thread(task);
                thread.start();
            }
        }
    }

    static class ElectionCell implements Callback<TableColumn.CellDataFeatures<QueryDocumentSnapshot, String>, ObservableValue<String>> {

        private CELL cell;

        enum CELL {
            UID,
            NAME,
            DATE,
        }

        private ElectionCell(CELL cell) {
            this.cell = cell;
        }

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<QueryDocumentSnapshot, String> param) {
            StringProperty value = new SimpleStringProperty();
            QueryDocumentSnapshot snapshot = param.getValue();
            Election election = snapshot.toObject(Election.class);

            switch (cell) {
                case UID:
                    value.setValue(snapshot.getId());
                    break;
                case NAME:
                    value.setValue(election.getTitle());
                    break;
                case DATE:
                    SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
                    value.setValue(format.format(election.getStart()));
                    break;
            }

            return value;
        }
    }

    class ActionButtonElection implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            ElectionFormDialog dialog = new ElectionFormDialog(null);
            Optional<Election> optional = dialog.showAndWait();

            optional.ifPresent(election -> firestore.collection("elections").add(election));
        }
    }

    class ActionButtonStudent implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            if (stage != null) {
                FXMLLoader loader = StudentFormController.getInstance();
                try {
                    Parent root = loader.load();
                    StudentFormController controller = loader.getController();
                    controller.setStage(stage);

                    stage.setScene(new Scene(root));
                    stage.setTitle("Student Form");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
