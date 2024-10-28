import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.geometry.Insets;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private SecretKey secretKey;
    private final int GCM_TAG_LENGTH = 128;
    private final int GCM_IV_LENGTH = 12;
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Secure File Management System");

        // Generate Secret Key for AES-GCM Encryption
        generateSecretKey();

        // Create GUI Elements
        Button uploadButton = new Button("Upload File");
        Button decryptButton = new Button("Decrypt File");
        Text statusText = new Text("Status: Ready");

        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    encryptAndUploadFile(file);
                    statusText.setText("Status: File encrypted and uploaded successfully.");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Encryption and upload failed.", ex);
                    statusText.setText("Error: " + ex.getMessage());
                }
            }
        });

        decryptButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Encrypted Files", "*.enc"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    decryptAndDownloadFile(file);
                    statusText.setText("Status: File decrypted and saved as 'secured'.");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Decryption and download failed.", ex);
                    statusText.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(uploadButton, decryptButton, statusText);

        Scene scene = new Scene(layout, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Method to Generate Secret Key for AES-GCM
    private void generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256); // Use 256-bit AES
            secretKey = keyGenerator.generateKey();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Secret key generation failed.", e);
        }
    }

    // Method to Encrypt and Upload File
    private void encryptAndUploadFile(File inputFile) throws Exception {
        byte[] fileContent = Files.readAllBytes(inputFile.toPath());

        // Generate random IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] encryptedData = cipher.doFinal(fileContent);

        // Save encrypted data and IV to a local file
        try (FileOutputStream fileOutputStream = new FileOutputStream(inputFile.getAbsolutePath() + ".enc")) {
            fileOutputStream.write(iv);
            fileOutputStream.write(encryptedData);
        }
    }

    // Method to Decrypt and Download File
    private void decryptAndDownloadFile(File encryptedFile) throws Exception {
        byte[] fileContent = Files.readAllBytes(encryptedFile.toPath());

        // Extract IV from the beginning of the file
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(fileContent, 0, iv, 0, GCM_IV_LENGTH);

        // Extract encrypted data
        byte[] encryptedData = new byte[fileContent.length - GCM_IV_LENGTH];
        System.arraycopy(fileContent, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        // Decrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);

        // Save decrypted data to a local file as "secured"
        File outputFile = new File(encryptedFile.getParent(), "secured");
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            fileOutputStream.write(decryptedData);
        }
    }
}
