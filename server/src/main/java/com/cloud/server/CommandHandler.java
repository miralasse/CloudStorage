package com.cloud.server;

import com.cloud.common.CmdMessage;
import com.cloud.common.FileInfo;
import com.cloud.common.FileListMessage;
import com.cloud.common.FileMessage;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler {

    public static void handle (ChannelHandlerContext ctx, CmdMessage msg, Path clientDirectory) {
        System.out.println("Client's command: " + msg.getCommand());
        if (msg.getCommand() != null) {
            switch (msg.getCommand()) {
                case DOWNLOAD_FILE_FROM_SERVER:
                    String fileToDownload = msg.getFileName();
                    System.out.println(fileToDownload);
                    if (fileToDownload != null) {
                        ctx.writeAndFlush(getFileMessage(fileToDownload, clientDirectory));
                        System.out.println("FileMessage sent");
                    }
                    break;

                case DELETE_FILE:
                    String fileToDelete = msg.getFileName();
                    if (fileToDelete != null) {
                        deleteFile(fileToDelete, clientDirectory);
                        sendFileList(ctx, clientDirectory);
                    }
                    break;

                case RENAME_FILE:
                    String oldFileName = msg.getFileName();
                    String newFileName = msg.getNewFileName();
                    if (oldFileName != null && newFileName != null) {
                        renameFile(oldFileName, newFileName, clientDirectory);
                        sendFileList(ctx,clientDirectory);
                    }
                    break;

                case REFRESH_FILE_LIST:
                    sendFileList(ctx, clientDirectory);
                    break;

                case CLIENT_EXIT:
                    ctx.close();
                    break;
            }
        }
    }

    private static List<FileInfo> getFileList (Path clientDirectory) {
        List<FileInfo> fileList = new ArrayList<>();
        try {
            fileList = Files.list(clientDirectory)
                    .map(path -> new FileInfo(path.getFileName().toString(), path.toFile().length()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    public static void sendFileList(ChannelHandlerContext ctx, Path clientDirectory) {
        FileListMessage listMessage = new FileListMessage(getFileList(clientDirectory));
        System.out.println("Содержимое сообщения FileListMessage на сервере: " + listMessage.getFileList());
        ctx.writeAndFlush(listMessage);
        System.out.println("Список файлов отправлен");
    }

    private static void deleteFile(String fileName, Path clientDirectory) {
        Path pathToDelete = Paths.get(clientDirectory + "/" + fileName);
        try {
            Files.delete(pathToDelete);
            System.out.println("Удален файл " + fileName);
        } catch (IOException e) {
            System.out.println("Deleting file failed");
            e.printStackTrace();
        }
    }

    private static void renameFile(String oldFileName, String newFileName, Path clientDirectory) {
        Path sourcePath = Paths.get(clientDirectory + "/" + oldFileName);
        Path destinationPath = Paths.get(clientDirectory + "/" + newFileName);
        try {
            Files.move(sourcePath, destinationPath,
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Переименован файл " + oldFileName + " в файл " + newFileName);
        } catch (IOException e) {
            System.out.println("Moving file failed");
            e.printStackTrace();
        }
    }

    private static FileMessage getFileMessage(String fileName, Path clientDirectory) {
        FileMessage fileMessage = new FileMessage();
        Path file = Paths.get(clientDirectory + "/" + fileName);
        fileMessage.setFileName(file.getFileName().toString());
        fileMessage.setFileSize(file.toFile().length());
        try {
            fileMessage.setContent(Files.readAllBytes(file));
            System.out.println("FileMessage is ready to be sent");
        } catch (IOException e) {
            System.out.println("Making FileMessage failed");
            e.printStackTrace();
        }
        return fileMessage;
    }

    public static void saveFileToStorage (FileMessage msg, Path clientDirectory) {
        if (msg.getFileName() == null || msg.getContent() == null)
            return;
        Path filePath = Paths.get(clientDirectory + "/" + msg.getFileName());
        byte[] content = msg.getContent();
        try {
            Files.write(filePath, content);
            System.out.println("File " + filePath.toString() + " has been uploaded");
        } catch (IOException e) {
            System.out.println("Uploading file failed");
            e.printStackTrace();
        }
    }

}
