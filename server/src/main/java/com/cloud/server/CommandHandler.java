package com.cloud.server;

import com.cloud.common.*;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
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
                        sendFilePartly(ctx, fileToDownload, clientDirectory);
                        System.out.println("File sent");
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

    private static void sendFilePartly(ChannelHandlerContext ctx, String fileName, Path clientDirectory) {
        FilePartMessage filePartMsg = new FilePartMessage();
        File file = Paths.get(clientDirectory + "/" + fileName).toFile();
        filePartMsg.setFileName(file.getName());
        int numOfParts = (int) Math.ceil((double) file.length()/Settings.MAX_FILE_PART_SIZE);
        byte[] contentPart;
        if (numOfParts == 1) {
            contentPart = new byte[(int)file.length()];
        } else {
            contentPart = new byte[Settings.MAX_FILE_PART_SIZE];
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (int i = 0; i < numOfParts; i++) {    //цикл отправки частей файла в сообщениях
                raf.seek(i * Settings.MAX_FILE_PART_SIZE);
                int bytesRead = raf.read(contentPart);

                filePartMsg.setPartNumber(i);
                filePartMsg.setNumOfParts(numOfParts);
                filePartMsg.setPartSize(bytesRead);
                filePartMsg.setContent(Arrays.copyOf(contentPart, bytesRead));

                System.out.println(filePartMsg.getFileName() + " : "
                        + filePartMsg.getNumOfParts() + " частей "
                        + filePartMsg.getPartNumber() + " - номер части "
                        + filePartMsg.getPartSize() + " байт размером");

                ctx.writeAndFlush(filePartMsg);
                Arrays.fill(contentPart, (byte) 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean saveFileToStorage(FilePartMessage msg, Path clientDirectory) {
        System.out.println("Server received file part");
        if (msg.getFileName() == null || msg.getContent() == null)
            return false;
        Path filePath = Paths.get(clientDirectory + "/" + msg.getFileName());
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw");
            System.out.println(msg.getFileName() + " "
                    + msg.getNumOfParts() + " частей "
                    + msg.getPartNumber() + " - номер части "
                    + msg.getPartSize() + " байт размером");

            raf.seek(msg.getPartNumber() * Settings.MAX_FILE_PART_SIZE);
            raf.write(msg.getContent());
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (msg.getPartNumber() == msg.getNumOfParts() - 1);
    }

}
