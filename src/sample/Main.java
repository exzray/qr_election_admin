package sample;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sample.controller.DashboardController;

import java.io.FileInputStream;

public class Main extends Application implements EventHandler<WindowEvent> {

    @Override
    public void start(Stage primaryStage) throws Exception {
        initFirebaseApp();

        // default view
        FXMLLoader loader = DashboardController.getInstance();
        Parent root = loader.load();
        DashboardController controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setTitle("QR Election Admin");
        primaryStage.setOnCloseRequest(this);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    private static void initFirebaseApp() throws Exception {
        FileInputStream serviceAccount =
                new FileInputStream(System.getProperty("user.dir") + "/src/key.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://qr-election.firebaseio.com")
                .build();

        FirebaseApp.initializeApp(options);
    }

    @Override
    public void handle(WindowEvent event) {
        // collect used listener and close them properly
        if (DashboardController.listener_election != null)
            DashboardController.listener_election.remove();
    }
}
