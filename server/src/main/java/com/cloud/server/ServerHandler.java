package com.cloud.server;

import com.cloud.common.CmdMessage;
import com.cloud.common.FileMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;
            System.out.println(msg.getClass());
            if (msg instanceof CmdMessage) {
                System.out.println("Client's command: " + ((CmdMessage) msg).getCommand());
                CmdMessage answer = new CmdMessage(CmdMessage.Command.DELETE_FILE_CONFIRM);
                ctx.write(answer);
            } else if (msg instanceof FileMessage) {
                String fileName = "new" + ((FileMessage) msg).getFileName();
                Path filePath = Paths.get(fileName);
                System.out.println("Received file: " + filePath.toString());
                byte[] content = ((FileMessage) msg).getContent();
                Files.write(filePath, content);
                System.out.println("File is ready");
            } else {
                System.out.println("Server received wrong object!");
                return;
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
}
