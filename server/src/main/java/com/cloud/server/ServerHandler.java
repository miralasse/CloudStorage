package com.cloud.server;

import com.cloud.common.CmdMessage;
import com.cloud.common.FileMessage;
import com.cloud.common.FilePartMessage;
import com.cloud.common.Settings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;


public class ServerHandler extends ChannelInboundHandlerAdapter {
    private Path clientDirectory;

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
                CommandHandler.handle(ctx, (CmdMessage) msg, clientDirectory);
            } else if (msg instanceof FileMessage) {
                CommandHandler.saveFileToStorage((FileMessage) msg, clientDirectory);
                CommandHandler.sendFileList(ctx, clientDirectory);

            } else if (msg instanceof FilePartMessage) {
                System.out.println("Server received file part");
                if ((((FilePartMessage) msg).getFileName() == null || ((FilePartMessage) msg).getContent() == null))
                    return;
                Path filePath = Paths.get(clientDirectory + "/" + ((FilePartMessage) msg).getFileName());
                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
                try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
                    System.out.println(((FilePartMessage) msg).getFileName() + " "
                            + ((FilePartMessage) msg).getNumOfParts() + "частей "
                            + ((FilePartMessage) msg).getPartNumber() + "- номер части "
                            + ((FilePartMessage) msg).getPartSize() + " байт размером");

                    System.out.println("Позиция " + ((FilePartMessage) msg).getPartNumber() * Settings.MAX_FILE_PART_SIZE);
                    raf.seek(((FilePartMessage) msg).getPartNumber() * Settings.MAX_FILE_PART_SIZE);
                    raf.write(((FilePartMessage) msg).getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new CmdMessage(CmdMessage.Command.SERVER_EXIT));
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
}
