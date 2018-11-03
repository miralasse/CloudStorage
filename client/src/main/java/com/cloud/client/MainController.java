package com.cloud.client;

import com.cloud.common.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

public class MainController implements Initializable {

    @FXML
    TableView<FileInfo> tableView;
    
    private ObservableList<FileInfo> fileList;

    private String fileNameFromTable;
    private String newFileName;
    private File storedFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileList = FXCollections.observableArrayList();

        TableColumn<FileInfo, String> tcFileName = new TableColumn<>("File");
        tcFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        TableColumn<FileInfo, String> tcFileSize = new TableColumn<>("Size");
        tcFileSize.setCellValueFactory(param -> {
            long size = param.getValue().getFileSize();
            return new ReadOnlyObjectWrapper(String.format("%,d", size) + " bytes");
        });

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

        //drag'n'drop
        tableView.setOnDragOver(event -> {
            if (event.getGestureSource() != tableView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        tableView.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                for (int i = 0; i < dragboard.getFiles().size(); i++) {
                    sendFilePartly(dragboard.getFiles().get(i));
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });



        listenToServer();
        askForFileList();
    }

    public void listenToServer() {
        Thread t = new Thread(() -> {
            try {
                while (true) {    //цикл получения сообщений
                    Message msg = Network.receiveMessage();
                    if (msg != null) {
                        System.out.println("Клиент получил сервера сообщение вида " + msg.getClass());
                        if (msg instanceof FileListMessage) {
                            updateFileList((FileListMessage) msg);
                        } else if (msg instanceof FilePartMessage) {
                            boolean lastPart = savePartFile((FilePartMessage) msg);
                            if (lastPart) {
                                storedFile = null;    //сброс файла для нового сохранения
                                System.out.println("File has been received");
                            }
                        } else if (msg instanceof CmdMessage) {
                            if (((CmdMessage) msg).getCommand() == CmdMessage.Command.SERVER_EXIT) {
                                break;
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.disconnect();
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
        final Runnable updateListRunnable = () -> {
            fileList.clear();
            fileList.addAll(msg.getFileList());
            System.out.println("Содержимое списка файлов: " + fileList);
            System.out.println("Список файлов обновлен на основании сообщения от сервера");
        };
        if (Platform.isFxApplicationThread()) {
            updateListRunnable.run();
        } else {
            Platform.runLater(updateListRunnable);
        }
    }

    public void uploadFile() {    //срабатывает по кнопке Загрузить файл
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload file");
        sendFilePartly(fileChooser.showOpenDialog(StageHelper.getStage()));
    }

    public void sendFilePartly(File selectedFile) {    //отправляет файл на сервер по частям
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
                for (int i = 0; i < numOfParts; i++) {        //цикл отправки частей файла в сообщениях
                    raf.seek(i * Settings.MAX_FILE_PART_SIZE);
                    int bytesRead = raf.read(contentPart);

                    filePartMsg.setPartNumber(i);
                    filePartMsg.setNumOfParts(numOfParts);
                    filePartMsg.setPartSize(bytesRead);
                    if (bytesRead == Settings.MAX_FILE_PART_SIZE) {
                        filePartMsg.setContent(contentPart);
                    } else {
                        filePartMsg.setContent(Arrays.copyOf(contentPart, bytesRead));
                    }
                    System.out.println(filePartMsg.getFileName() + " : "
                            + filePartMsg.getNumOfParts() + " частей "
                            + filePartMsg.getPartNumber() + " - номер части "
                            + filePartMsg.getPartSize() + " байт размером");

                    Network.sendMessage(filePartMsg);
                    //Arrays.fill(contentPart, (byte) 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void askForDownload() {    //запрашивает файл у сервера
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save file");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*"));
            fileChooser.setInitialFileName(fileNameFromTable);
            storedFile = fileChooser.showSaveDialog(StageHelper.getStage());
            System.out.println("Файл для сохранения: " + storedFile);
            if (storedFile != null) {
                CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DOWNLOAD_FILE_FROM_SERVER);
                cmdMessage.setFileName(fileNameFromTable);
                Network.sendMessage(cmdMessage);
                System.out.println("Клиент запросил у сервера файл " + fileNameFromTable);
            }
        });

    }

    private boolean savePartFile(FilePartMessage msg) {    //сохраняет часть файла, пришедшую с сервера
        System.out.println("Метод savePartFile вызван");
        if (msg.getFileName() == null || msg.getContent() == null)
            return false;
        if (storedFile != null) {
            Path filePath = storedFile.toPath();
            try {
                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
                RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw");
                raf.seek(msg.getPartNumber() * Settings.MAX_FILE_PART_SIZE);
                raf.write(msg.getContent());
                raf.close();
                System.out.println("Часть файла записана");
                return (msg.getPartNumber() == msg.getNumOfParts() - 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Saving file cancelled");
        }
        return false;
    }

    public void deleteFile() {    //запрашивает удаление файла у сервера
        if (fileNameFromTable != null) {
            CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DELETE_FILE);
            cmdMessage.setFileName(fileNameFromTable);
            Network.sendMessage(cmdMessage);
            System.out.println("Клиент запросил у сервера удаление файла " + fileNameFromTable);
        }
    }

    public void renameFile() {    //запрашивает переименование файла у сервера
        if (fileNameFromTable != null || newFileName != null) {
            CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.RENAME_FILE);
            cmdMessage.setFileName(fileNameFromTable);
            cmdMessage.setNewFileName(newFileName);
            Network.sendMessage(cmdMessage);
            System.out.println("Клиент запросил у сервера переименование файла " + fileNameFromTable + " в имя " + newFileName);
        }
    }

    public void enterNewFileName() {    //запрашивает новое имя файла у пользователя при переименовании файла
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
