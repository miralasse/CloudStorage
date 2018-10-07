package com.cloud.client;

import com.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class MainController implements Initializable {

    @FXML
    TableView<FileInfo> tableView;

    private boolean closedByClient;

    private ObservableList<FileInfo> fileList;

    private String fileNameFromTable;
    private String newFileName;

    private StageHelper helper;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        helper = StageHelper.getInstance();

        fileList = FXCollections.observableArrayList();

        TableColumn<FileInfo, String> tcFileName = new TableColumn<>("File");
        tcFileName.setCellValueFactory(new PropertyValueFactory<>("file"));

        TableColumn<FileInfo, String> tcFileSize = new TableColumn<>("Size");
        tcFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        tableView.getColumns().addAll(tcFileName, tcFileSize);
        tableView.setItems(fileList);

        tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.getClickCount() == 2) {
                    fileNameFromTable = tableView.getSelectionModel().getSelectedItem().getFileName();
                    System.out.println("File " + fileNameFromTable + " was chosen");
                }
            }
        });
        listenToServer();
        askForFileList();
    }

    public void listenToServer(){
       Thread t = new Thread(new Runnable() {
           @Override
           public void run() {
               try {
                   while (!closedByClient) {    //цикл получения сообщений
                       Message msg = Network.receiveMessage();
                       if (msg == null)
                           return;
                       System.out.println(msg.getClass());
                       if (msg instanceof FileListMessage) {
                           updateFileList((FileListMessage) msg);
                       } else if (msg instanceof FileMessage) {
                           saveFile((FileMessage) msg);
                       } else {
                           System.out.println("Client received wrong object!");
                       }
                   }
               } finally {
                   Network.disconnect();
               }
           }
       });
            t.setDaemon(true);
            t.start();
    }

    public void askForFileList() {    //запращивает у сервера обновленный список файлов
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.REFRESH_FILE_LIST);
        Network.sendMessage(cmdMessage);
    }

    public void updateFileList(FileListMessage msg) {  //обновляет список файлов на основании данных от сервера
        if (Platform.isFxApplicationThread()) {
            fileList.clear();
            fileList.addAll(msg.getFileList());
        } else {
            Platform.runLater(() -> {
                fileList.clear();
                fileList.addAll(msg.getFileList());
            });
        }
    }

    public void uploadFile() {    //отправляет файл на сервер

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Upload file");
            File selectedFile = fileChooser.showOpenDialog(helper.getStage());
            if (selectedFile != null) {
                FileMessage fileMessage = new FileMessage();
                Path uploadingFile = selectedFile.toPath();
                fileMessage.setFileName(uploadingFile.getFileName().toString());
                fileMessage.setFileSize(uploadingFile.toFile().length());
                try {
                    fileMessage.setContent(Files.readAllBytes(uploadingFile));
                } catch (IOException e) {
                    System.out.println("Writing FileMessage failed");
                    e.printStackTrace();
                }
                Network.sendMessage(fileMessage);
            } else {
                System.out.println("Uploading file cancelled");
            }
    }

    public void downloadFile() {    //запрашивает файл у сервера
        if (fileNameFromTable == null)
            return;
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DOWNLOAD_FILE_FROM_SERVER);
        cmdMessage.getCommand().setFileName(fileNameFromTable);
        Network.sendMessage(cmdMessage);

    }

    private void saveFile(FileMessage msg) {    //сохраняет файл, пришедший с сервера
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save file");
        fileChooser.setInitialFileName(msg.getFileName());
        File savedFile = fileChooser.showSaveDialog(helper.getStage());
        if (savedFile != null) {
            Path filePath = savedFile.toPath();
            System.out.println("Received file: " + filePath.toString());
            byte[] content = msg.getContent();
            try {
                Files.write(filePath, content, StandardOpenOption.CREATE_NEW);
                System.out.println("File has been saved");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Saving file failed");
            }
        } else {
            System.out.println("Saving file cancelled");
        }
    }

    public void deleteFile() {    //запрашивает удаление файла у сервера
        if (fileNameFromTable == null)
            return;
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DELETE_FILE);
        cmdMessage.getCommand().setFileName(fileNameFromTable);
        Network.sendMessage(cmdMessage);
    }

    public void renameFile() {    //запрашивает переименование файла у сервера
        if (fileNameFromTable == null || newFileName == null)
            return;
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.RENAME_FILE);
        cmdMessage.getCommand().setFileName(fileNameFromTable);
        cmdMessage.getCommand().setNewFileName(newFileName);
        Network.sendMessage(cmdMessage);
    }

    public void enterNewFileName() {    //запрашивает новое имя файла у пользователя
        TextInputDialog dialog = new TextInputDialog("renaming");
        dialog.setTitle("Rename the file");
        dialog.setHeaderText("Renaming the file " + fileNameFromTable);
        dialog.setContentText("Please enter new name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            newFileName = result.get();
            System.out.println("New file name: " + result.get());
        }
        renameFile();
    }
}
