package sample.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import sample.model.Campaign;
import sample.utility.ControllerExtended;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CampaignController extends ControllerExtended implements Initializable {

    private static ListenerRegistration listener_campaign;

    private QueryDocumentSnapshot election_snapshot;
    private QueryDocumentSnapshot campaign_snapshot;
    private ObservableList<QueryDocumentSnapshot> campaigns = FXCollections.observableArrayList();

    // widget
    @FXML
    private ListView<QueryDocumentSnapshot> list_campaign;
    @FXML
    private Button button_clear, button_remove, button_submit, button_position;
    @FXML
    private JFXTextField field_title, field_description, field_image;


    public static FXMLLoader getInstance() {
        return new FXMLLoader(DashboardController.class.getResource("../layout/campaign_form.fxml"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // clear firebase task
        if (listener_campaign != null) listener_campaign.remove();

        list_campaign.setItems(campaigns);
        list_campaign.setCellFactory(param -> new CampainListCell());
        list_campaign.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        BooleanBinding binding = list_campaign.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding binding1 = Bindings.isEmpty(field_title.textProperty())
                .or(Bindings.isEmpty(field_description.textProperty()))
                .or(Bindings.isEmpty(field_image.textProperty()));

        button_clear.disableProperty().bind(binding);
        button_remove.disableProperty().bind(binding);
        button_position.disableProperty().bind(binding);
        button_submit.disableProperty().bind(binding1);

        button_remove.setOnAction(event -> {
            QueryDocumentSnapshot snapshot = list_campaign.getSelectionModel().getSelectedItem();
            snapshot.getReference().delete();

            campaigns.remove(snapshot);
        });
        button_clear.setOnAction(event -> {
            list_campaign.getSelectionModel().clearSelection();
            campaign_snapshot = null;

            // empty field
            field_title.clear();
            field_description.clear();
            field_image.clear();
        });
        button_submit.setOnAction(event -> {
            Campaign campaign;

            if (campaign_snapshot == null) campaign = new Campaign();
            else campaign = campaign_snapshot.toObject(Campaign.class);

            String str_title = field_title.getText();
            String str_image = field_image.getText();
            String str_description = field_description.getText();

            campaign.setTitle(str_title);
            campaign.setImage(str_image);
            campaign.setDescription(str_description);

            if (campaign_snapshot == null) {
                election_snapshot
                        .getReference()
                        .collection("campaigns")
                        .add(campaign);
            } else {
                campaign_snapshot
                        .getReference()
                        .set(campaign);
            }

            list_campaign.getSelectionModel().clearSelection();
            campaign_snapshot = null;

            // empty field
            field_title.clear();
            field_description.clear();
            field_image.clear();
        });
        button_position.setOnAction(event -> {
            FXMLLoader loader = PositionFormController.getInstance();

            try {
                Parent parent = loader.load();
                PositionFormController controller = loader.getController();
                controller.setStage(stage);
                controller.setCampaign(campaign_snapshot);

                stage.setScene(new Scene(parent));
                stage.setTitle(campaign_snapshot.toObject(Campaign.class).getTitle());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public void setElection(QueryDocumentSnapshot election_snapshot) {
        this.election_snapshot = election_snapshot;

        listenToCampaign();
    }

    private void listenToCampaign() {
        DocumentReference reference = election_snapshot.getReference();

        listener_campaign = reference
                .collection("campaigns")
                .addSnapshotListener((value, error) -> {

                    if (value != null) {
                        List<QueryDocumentSnapshot> list = value.getDocuments();

                        synchronized (this) {
                            campaigns.clear();
                            Platform.runLater(() -> campaigns.addAll(list));
                        }
                    }
                });
    }

    class CampainListCell extends ListCell<QueryDocumentSnapshot> {
        @Override
        protected void updateItem(QueryDocumentSnapshot item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                Campaign campaign = item.toObject(Campaign.class);

                setText(campaign.getTitle());

                setOnMouseClicked(event -> {
                    campaign_snapshot = item;

                    field_title.setText(campaign.getTitle());
                    field_description.setText(campaign.getDescription());
                    field_image.setText(campaign.getImage());
                });
            }
        }
    }
}
