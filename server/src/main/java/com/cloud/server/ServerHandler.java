package com.cloud.server;

import com.cloud.common.CmdMessage;
import com.cloud.common.FileInfo;
import com.cloud.common.FileListMessage;
import com.cloud.common.FileMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;


public class ServerHandler extends ChannelInboundHandlerAdapter {
    private Path clientDirectory;
    private ArrayList<FileInfo> fileList = new ArrayList<>();

    public ServerHandler(String login) {
        checkRepo(login);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client " + clientDirectory + " has connected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;
            System.out.println(msg.getClass());
            if (msg instanceof CmdMessage) {
                CmdMessage.Command command = ((CmdMessage) msg).getCommand();
                System.out.println("Client's command: " + command);
                if (command == null)
                    return;
                switch (command) {
                    case DOWNLOAD_FILE_FROM_SERVER:
                        String fileToDownload = ((CmdMessage) msg).getFileName();
                        System.out.println(fileToDownload);
                        if (fileToDownload == null)
                            return;
                        ctx.write(getFile(fileToDownload));
                        ctx.flush();
                        System.out.println("FileMessage sent");
                        break;

                    case DELETE_FILE:
                        String fileToDelete = ((CmdMessage) msg).getFileName();
                        if (fileToDelete == null)
                            return;
                        deleteFile(fileToDelete);
                        sendFileList(ctx);
                        break;

                    case RENAME_FILE:
                        String oldFileName = ((CmdMessage) msg).getFileName();
                        String newFileName = ((CmdMessage) msg).getNewFileName();
                        if (oldFileName == null || newFileName == null)
                            return;
                        renameFile(oldFileName, newFileName);
                        sendFileList(ctx);
                        break;

                    case REFRESH_FILE_LIST:
                        sendFileList(ctx);
                        break;
                }
            } else if (msg instanceof FileMessage) {
                saveFileToStorage((FileMessage) msg);
                sendFileList(ctx);
            } else {
                System.out.println("Server received wrong object!");
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void checkRepo (String login) {
        Path path = Paths.get("server/repository/" + login);
        if (Files.exists(path)) {
            clientDirectory = path;
        } else {
            try {
                clientDirectory = Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getFileList() {
        fileList.clear();
        try {
            Files.walkFileTree(clientDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println(file.getFileName().toString() + " " + file.toFile().length() + " байт");
                    fileList.add(new FileInfo(file.getFileName().toString(), file.toFile().length()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("Getting file list failed");
            e.printStackTrace();
        }
        System.out.println("Список файлов обновлен. Содержимое списока: " + fileList);
    }

    private void sendFileList(ChannelHandlerContext ctx) {
        getFileList();
        FileListMessage listMessage = new FileListMessage();
        listMessage.setFileList(fileList);
        System.out.println("Содержимое сообщения FileListMessage на сервере: " + listMessage.getFileList());
        ctx.write(listMessage);
        ctx.flush();
        System.out.println("Список файлов отправлен");
    }

    private void deleteFile(String fileName) {
        Path pathToDelete = Paths.get(clientDirectory + "/" + fileName);
        try {
            Files.delete(pathToDelete);
            System.out.println("Удален файл " + fileName);
        } catch (IOException e) {
            System.out.println("Deleting file failed");
            e.printStackTrace();
        }
    }

    private void renameFile(String oldFileName, String newFileName) {
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

    private void saveFileToStorage (FileMessage msg) {
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

    private FileMessage getFile(String fileName) {
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

}
