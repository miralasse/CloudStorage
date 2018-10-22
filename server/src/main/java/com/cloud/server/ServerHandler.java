package com.cloud.server;

import com.cloud.common.CmdMessage;
import com.cloud.common.FilePartMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
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
            if (msg instanceof CmdMessage) {
                CommandHandler.handle(ctx, (CmdMessage) msg, clientDirectory);
            } else if (msg instanceof FilePartMessage) {
                boolean lastPart = CommandHandler.saveFileToStorage((FilePartMessage) msg, clientDirectory);
                if (lastPart)
                    CommandHandler.sendFileList(ctx, clientDirectory);
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
