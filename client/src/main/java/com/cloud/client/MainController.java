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
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class MainController implements Initializable {

    @FXML
    TableView<FileInfo> tableView;
    
    private ObservableList<FileInfo> fileList;

    private String fileNameFromTable;
    private String newFileName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileList = FXCollections.observableArrayList();

        TableColumn<FileInfo, String> tcFileName = new TableColumn<>("File");
        tcFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        TableColumn<FileInfo, String> tcFileSize = new TableColumn<>("Size");
        tcFileSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));

        tableView.getColumns().addAll(tcFileName, tcFileSize);
        tableView.setItems(fileList);

        tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() >= 1) {
                    if (tableView.getSelectionModel().getSelectedItem() != null) {
                        fileNameFromTable = tableView.getSelectionModel().getSelectedItem().getFileName();
                        System.out.println("Пользователь выбрал в таблице файл " + fileNameFromTable);
                    }
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
                   while (true) {    //цикл получения сообщений
                       Message msg = Network.receiveMessage();
                       if (msg != null) {
                           System.out.println("Клиент получил сервера сообщение вида " + msg.getClass());
                           if (msg instanceof FileListMessage) {
                               System.out.println("Содержимое сообщения FileListMessage на клиенте: " + ((FileListMessage) msg).getFileList());
                               updateFileList((FileListMessage) msg);
                           } else if (msg instanceof FileMessage) {
                               saveFile((FileMessage) msg);
                           } else if (msg instanceof CmdMessage){
                               if (((CmdMessage) msg).getCommand() == CmdMessage.Command.SERVER_EXIT) {
                                   break;
                               }
                           }
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
        System.out.println("Клиент запросил у сервера обновленный список файлов");
    }

    public void updateFileList(FileListMessage msg) {  //обновляет список файлов на основании данных от сервера
        if (Platform.isFxApplicationThread()) {
            fileList.clear();
            fileList.addAll(msg.getFileList());
            System.out.println("Содержимое списка файлов: " + fileList);
            System.out.println("Список файлов обновлен на основании сообщения от сервера");
        } else {
            Platform.runLater(() -> {
                fileList.clear();
                fileList.addAll(msg.getFileList());
                System.out.println("Содержимое списка файлов: " + fileList);
                System.out.println("Список файлов обновлен на основании сообщения от сервера в runLater");
            });
        }
    }

    public void uploadFile() {    //отправляет файл на сервер
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Upload file");
            File selectedFile = fileChooser.showOpenDialog(StageHelper.getStage());
            if (selectedFile != null) {
                FileMessage fileMessage = new FileMessage();
                Path uploadingFile = selectedFile.toPath();
                fileMessage.setFileName(uploadingFile.getFileName().toString());
                System.out.println("Клиент выбрал для загрузки на сервер файл " + uploadingFile.getFileName());
                fileMessage.setFileSize(uploadingFile.toFile().length());
                try {
                    fileMessage.setContent(Files.readAllBytes(uploadingFile));
                    System.out.println("Контент файла записан для отправки");
                } catch (IOException e) {
                    System.out.println("Writing FileMessage failed");
                    e.printStackTrace();
                }
                Network.sendMessage(fileMessage);
                System.out.println("Файл отправлен на сервер");
            } else {
                System.out.println("Uploading file cancelled");
            }
    }

    public void uploadFilePartly() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload file");
        File selectedFile = fileChooser.showOpenDialog(StageHelper.getStage()); //выбрали файл
        if (selectedFile != null) {
            FilePartMessage filePartMsg = new FilePartMessage(); //создали объект сообщения
            filePartMsg.setFileName(selectedFile.getName());
            System.out.println("Клиент выбрал для загрузки на сервер файл " + selectedFile.getName());
            int numOfParts = (int) Math.ceil((double) selectedFile.length()/Settings.MAX_FILE_PART_SIZE);
            byte[] contentPart;
            if (numOfParts == 1) {
                contentPart = new byte[(int)selectedFile.length()];
            } else {
                contentPart = new byte[Settings.MAX_FILE_PART_SIZE];
            }
            try (RandomAccessFile raf = new RandomAccessFile(selectedFile, "r")) {
                for (int i = 0; i < numOfParts; i++) {    //цикл отправки частей файла в сообщениях
                    raf.seek(i * Settings.MAX_FILE_PART_SIZE);
                    int bytesRead = raf.read(contentPart);
                    filePartMsg.setPartNumber(i);
                    filePartMsg.setNumOfParts(numOfParts);
                    filePartMsg.setPartSize(bytesRead);
                    filePartMsg.setContent(Arrays.copyOf(contentPart, bytesRead));
                    System.out.println(filePartMsg.getFileName() + " "
                            + filePartMsg.getNumOfParts() + "частей "
                            + filePartMsg.getPartNumber() + "- номер части "
                            + filePartMsg.getPartSize() + " байт размером");

                    Network.sendMessage(filePartMsg);
                    Arrays.fill(contentPart, (byte) 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void askForDownload() {    //запрашивает файл у сервера
        System.out.println("askForDownload запущен");
        if (fileNameFromTable == null)
            return;
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DOWNLOAD_FILE_FROM_SERVER);
        cmdMessage.setFileName(fileNameFromTable);
        Network.sendMessage(cmdMessage);
        System.out.println(cmdMessage);
        System.out.println("Значение поля имя файла из сообщения: " + cmdMessage.getFileName());
        System.out.println("Клиент запросил у сервера файл " + fileNameFromTable);
    }

    private void saveFile(FileMessage msg) {    //сохраняет файл, пришедший с сервера
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save file");
            fileChooser.setInitialFileName(msg.getFileName());
            File savedFile = fileChooser.showSaveDialog(StageHelper.getStage());
            if (savedFile != null) {
                Path filePath = savedFile.toPath();
                System.out.println("Клиент сохраняет файл " + filePath.toString());
                byte[] content = msg.getContent();
                try {
                    Files.write(filePath, content);
                    System.out.println("Файл сохранен");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Saving file failed");
                }
            } else {
                System.out.println("Saving file cancelled");
            }
        });
    }

    public void deleteFile() {    //запрашивает удаление файла у сервера
        if (fileNameFromTable == null)
            return;
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DELETE_FILE);
        cmdMessage.setFileName(fileNameFromTable);
        Network.sendMessage(cmdMessage);
        System.out.println("Клиент запросил у сервера удаление файла " + fileNameFromTable);
    }

    public void renameFile() {    //запрашивает переименование файла у сервера
        if (fileNameFromTable == null || newFileName == null)
            return;
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.RENAME_FILE);
        cmdMessage.setFileName(fileNameFromTable);
        cmdMessage.setNewFileName(newFileName);
        Network.sendMessage(cmdMessage);
        System.out.println("Клиент запросил у сервера переименование файла " + fileNameFromTable + " в имя " + newFileName);
    }

    public void enterNewFileName() {    //запрашивает новое имя файла у пользователя
        TextInputDialog dialog = new TextInputDialog(fileNameFromTable);
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
