/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.a4l.joanbot;

import com.a4l.joanbot.util.DriverHandler;
import com.a4l.joanbot.util.FileManager;
import com.a4l.joanbot.util.KeyWordGenerator;
import com.sun.javafx.scene.control.skin.TextAreaSkin;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import java.io.IOException;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import org.openqa.selenium.WebDriver;

/**
 * FXML Controller class
 *
 * @author Henrique
 */
public class ControllerFX implements Initializable {
    private WebDriver driver;
    private File currentFile = null;
    
    public void setDriver(WebDriver driver){
        this.driver = driver;
    }
    
    @FXML private AnchorPane root;
    
    @FXML private Button sendBtn;
    @FXML private Button saveBtn;
    @FXML private ComboBox categorias;
    @FXML private TextField tTitulo;
    @FXML private TextField tSubtitulo;
    @FXML private TextField tFuentes;
    @FXML private TextField tEtiquetas;
    @FXML private TextArea tNoticia;
    @FXML private Label tNoticiaCount;
    @FXML private Label tTituloCount;
    @FXML private Label tSubtituloCount;
    
    @FXML private TextField tUser;
    @FXML private TextField tPasswd;
    
    @FXML private MenuItem mSalir;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList();
        for (Categorias cat : Categorias.values()){
            options.add(cat.getName());
        }
        
        categorias.getItems().addAll(options);
        categorias.getSelectionModel().selectFirst();
        tNoticia.setWrapText(true);
        tNoticiaCount.setOpacity(0);
        tTitulo.setTextFormatter(new TextFormatter(maxLength(80)));
        tSubtitulo.setTextFormatter(new TextFormatter(maxLength(150)));
        
        showUiCount(tTitulo, tTituloCount, 30);
        showUiCount(tSubtitulo, tSubtituloCount, 70);
        showUiCount(tNoticia, tNoticiaCount, 2000);
        
        // Hace que tabule el área de texto
        tNoticia.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
        @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.TAB)) {
                Node node = (Node) event.getSource();
                    if (node instanceof TextArea) {
                        TextAreaSkin skin = (TextAreaSkin) ((TextArea)node).getSkin();
                        if (event.isShiftDown()) {
                            skin.getBehavior().traversePrevious();
                        }
                        else {
                            skin.getBehavior().traverseNext();
                        }
                    } 
                event.consume();
                }
            }
        });
    } 
    
    @FXML private void sendNoticia(ActionEvent event){
        
        if(DriverHandler.isLogged(driver)){
            String categoria, titulo, subtitulo, noticia, fuentes, etiquetas;
            categoria = (String)categorias.getValue();
            titulo = tTitulo.getText();
            subtitulo = tSubtitulo.getText();
            noticia = tNoticia.getText();
            fuentes = tFuentes.getText();
            etiquetas = tEtiquetas.getText();
            
            if (isCorrect(titulo, subtitulo, noticia, etiquetas, fuentes, categoria)){
				try {
					KeyWordGenerator kwg = new KeyWordGenerator(titulo, subtitulo, noticia, etiquetas);
					kwg.calcularKeyWord();

					String urlP, urlS;
					urlP = DriverHandler.fastSearch(kwg.getKeyPrimaria(), driver);
					urlS = DriverHandler.fastSearch(kwg.getKeySecundaria(), driver);

					driver.get("http://blast.blastingnews.com/news/edit/");
					Thread.sleep(250);

					DriverHandler.setCategory(driver, categoria);
					DriverHandler.writeTitle(driver, titulo);
					DriverHandler.writeSubtitle(driver, subtitulo);
					DriverHandler.setPhoto(driver);
					DriverHandler.setNoBlasterHelp(driver);

					buildNoticia(noticia, kwg.getKeyPrimaria(), kwg.getKeySecundaria(), urlP, urlS);

					DriverHandler.setEtiquetas(driver, getEtiquetas(etiquetas));
					DriverHandler.writeFuentes(driver, fuentes);
					DriverHandler.sendNews(driver);

					DriverHandler.takeScreenshot(driver);

				} catch (InterruptedException ex) {
					Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
				} catch (IOException ex) {
					Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
				} 
            }
            
            else {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Rellena correctamente todos los campos");

                alert.showAndWait();
            }
        }
        
        else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Debes iniciar sesión primero");

            alert.showAndWait();
        }
    }
    
    @FXML private void logIn(ActionEvent e) throws InterruptedException{
        if (DriverHandler.isLogged(driver)){
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Ya has iniciado sesión");

            alert.showAndWait();
        }
        
        else {
            String user = tUser.getText();
            String passwd = tPasswd.getText();

            DriverHandler.login(user, passwd, driver);
            DriverHandler.closePopUp(driver);
        }
    }
    
    @FXML private void newNoticia(ActionEvent e){
        if (currentFile != null){
            Alert alert = new Alert(AlertType.CONFIRMATION, "¿Deseas guardar antes de abrir una nueva noticia?");
            Button exitButton = (Button) alert.getDialogPane().lookupButton(
                    ButtonType.OK
            );
            exitButton.setText("Guardar");
            alert.setHeaderText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(root.getScene().getWindow());

            Optional<ButtonType> closeResponse = alert.showAndWait();
            if (ButtonType.OK.equals(closeResponse.get())) {
                saveBtn.fireEvent(new ActionEvent());
            }
        }
        
        categorias.getSelectionModel().selectFirst();
        tTitulo.clear();
        tSubtitulo.clear();
        tNoticia.clear();
        tEtiquetas.clear();
        tFuentes.clear();
        
        currentFile = null;
    }
    @FXML private void saveNoticia(ActionEvent e){
        String categoria, titulo, subtitulo, noticia, fuentes, etiquetas;
        categoria = (String)categorias.getValue();
        titulo = tTitulo.getText();
        subtitulo = tSubtitulo.getText();
        noticia = tNoticia.getText();
        fuentes = tFuentes.getText();
        etiquetas = tEtiquetas.getText();
        
        Noticias nuevaNoticia = new Noticias(titulo, subtitulo, noticia, categoria, etiquetas, fuentes);
        
        if (currentFile == null){
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecciona el destino de la nueva noticia");
            chooser.setInitialFileName("NuevaNoticia.dat");
            chooser.setInitialDirectory(new File("./"));
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Noticias (*.dat)", "*.dat");
            chooser.getExtensionFilters().add(extFilter);
            currentFile = chooser.showSaveDialog(root.getScene().getWindow());
            
            if (currentFile != null)
                FileManager.writeFile(currentFile, nuevaNoticia);
        }
        
        else {
            FileManager.modifyFile(currentFile, nuevaNoticia);
        }
    }
    
    @FXML private void saveAsNoticia(ActionEvent e){
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecciona el destino de la nueva noticia");
        chooser.setInitialFileName("NuevaNoticia.dat");
        chooser.setInitialDirectory(new File("./"));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Noticias (*.dat)", "*.dat");
        chooser.getExtensionFilters().add(extFilter);
        
        currentFile = chooser.showSaveDialog(root.getScene().getWindow());
        
        if (currentFile != null){
            String categoria, titulo, subtitulo, noticia, fuentes, etiquetas;
            categoria = (String)categorias.getValue();
            titulo = tTitulo.getText();
            subtitulo = tSubtitulo.getText();
            noticia = tNoticia.getText();
            fuentes = tFuentes.getText();
            etiquetas = tEtiquetas.getText();
        
            Noticias nuevaNoticia = new Noticias(titulo, subtitulo, noticia, categoria, etiquetas, fuentes);
            
            FileManager.modifyFile(currentFile, nuevaNoticia);
        }
    }
    
    @FXML private void openNoticia(ActionEvent e){
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecciona una noticia a abrir");
        chooser.setInitialDirectory(new File("./"));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Noticias (*.dat)", "*.dat");
        chooser.getExtensionFilters().add(extFilter);
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        
        if (file != null){
            Noticias noticia = FileManager.readNoticia(file);

            tTitulo.setText(noticia.getTitulo());
            tSubtitulo.setText(noticia.getSubtitulo());
            tNoticia.setText(noticia.getNoticia());
            tEtiquetas.setText(noticia.getEtiquetas());
            tFuentes.setText(noticia.getFuentes());

            if (noticia.getCategoria() != null)        
                categorias.setValue(noticia.getCategoria());
            else
                categorias.getSelectionModel().selectFirst();
            
            currentFile = file;
        }
    }
    
    // TODO: ¿Estás seguro de que deseas salir, coño?
    @FXML private void mSalirAction(ActionEvent e){
        Platform.exit();
    }
    
    private void buildNoticia(String noticia, String keyPrimaria, String keySecundaria, String urlP, String urlS) throws InterruptedException, IOException{
        int indexKP, lastIndexKP, indexKS, lastIndexKS;
        StringBuilder builder = new StringBuilder(noticia);
        
        indexKP = builder.indexOf(keyPrimaria);
        lastIndexKP = builder.lastIndexOf(keyPrimaria);
        indexKS = builder.indexOf(keySecundaria);
        lastIndexKS = builder.lastIndexOf(keySecundaria);
        
        System.out.println(indexKP);
        System.out.println(indexKS);
        System.out.println(lastIndexKP);
        System.out.println(lastIndexKS);
        
        String subNoticia, subNoticia2, subNoticia3;

        subNoticia = builder.substring(0, indexKP);
        subNoticia2 = builder.substring(indexKP + keyPrimaria.length(), indexKS);
        subNoticia3 = builder.substring(indexKS + keySecundaria.length());
        
        System.out.println(subNoticia2);
        
        DriverHandler.writeNews(driver, subNoticia);
        DriverHandler.addLink(driver, urlP, keyPrimaria);
        Thread.sleep(50);
        DriverHandler.writeNews(driver, subNoticia2);
        DriverHandler.addLink(driver, urlS, keySecundaria);
        DriverHandler.writeNews(driver, subNoticia3);
    }
    
    private UnaryOperator<Change> maxLength(int len){
        UnaryOperator<Change> rejectChange = c -> {
            // check if the change might effect the validating predicate
            if (c.isContentChange()) {
                // check if change is valid
                String res = c.getControlNewText();
                if (countStringLength(res, true) > len) {
                    // invalid change
                    // return null to reject the change
                    Toolkit.getDefaultToolkit().beep();
                    return null;
                }
            }
            // valid change: accept the change by returning it
            return c;
        };
        
        return rejectChange;
    }
    
    // Si space = true cuenta los espacios no consecutivos, a no ser que esté al final o principio 
    // de la cadena
    private int countStringLength(String str, boolean space){
        if (!space){
            String res = str.replaceAll("\\s", "");
            return res.length();
        }
        
        else {
            String res = str.replaceAll("\\s+", " ");
            res = res.trim();
            return res.length();
        }
    }
    
    private void showUiCount (TextField text, Label count, int min){
        text.textProperty().addListener((observable, oldValue, newValue) -> {
            int caracteres = countStringLength(newValue, true);
            if (caracteres == 0 || caracteres >= min){ 
                if (count.getOpacity() != 0)
                    count.setOpacity(0);
            }
            else if (caracteres == 1){
                if (count.getOpacity() != 1)
                    count.setOpacity(1);
                
                count.setText("Has escrito " + caracteres + " carácter");
            }
            else if (caracteres < min){
                if (count.getOpacity() != 1)
                    count.setOpacity(1);
                
                count.setText("Has escrito " + caracteres + " caracteres");
            }
        });
    }
    
    private void showUiCount (TextArea text, Label count, int min){
        text.textProperty().addListener((observable, oldValue, newValue) -> {
            int caracteres = countStringLength(newValue, false);
            if (caracteres == 0 || caracteres >= min){ 
                if (count.getOpacity() != 0)
                    count.setOpacity(0);
            }
            else if (caracteres == 1){
                if (count.getOpacity() != 1)
                    count.setOpacity(1);
                
                count.setText("Has escrito " + caracteres + " carácter");
            }
            else if (caracteres < min){
                if (count.getOpacity() != 1)
                    count.setOpacity(1);
                
                count.setText("Has escrito " + caracteres + " caracteres");
            }
        });
    }
    
    private String[] getEtiquetas(String etiquetas){
      String[] res = etiquetas.split(",");
      
      for (int i = 0; i < res.length; i++){
          res[i].trim();
      }
      
      return res;
    }
    
    private boolean isCorrect(String titulo, String subtitulo, String noticia, String etiquetas, String fuentes, String categoria){
        return  (countStringLength(titulo, true) >= 30 &&
                countStringLength(subtitulo, true) >= 70 &&
                countStringLength(noticia, false) >= 2000 &&
                !categoria.isEmpty() &&
                !etiquetas.isEmpty() &&
                !fuentes.isEmpty()); 
    }
}