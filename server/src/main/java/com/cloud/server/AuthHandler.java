package com.cloud.server;

import com.cloud.common.AuthMessage;
import com.cloud.common.CmdMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authorized;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("New client is connecting and trying to authorize");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }
        if (!authorized) {
            if (msg instanceof AuthMessage) {
                System.out.println("AuthMessage received");
                String login = ((AuthMessage) msg).getLogin();
                String password = ((AuthMessage) msg).getPassword();
                AuthService authService = AuthService.getOurInstance();
                if (authService.checkLoginAndPass(login, password)) {
                    authorized = true;
                    System.out.println("Client " + login + " authorized successfully");
                    CmdMessage authOkMsg = new CmdMessage(CmdMessage.Command.AUTH_CONFIRM);
                    ctx.writeAndFlush(authOkMsg);
                    //ctx.pipeline().remove(this.getClass());
                    ctx.pipeline().addLast(new ServerHandler(login));
                } else {
                    System.out.println("Client's authorization data is incorrect");
                    CmdMessage authWrongMsg = new CmdMessage(CmdMessage.Command.AUTH_WRONG);
                    ctx.writeAndFlush(authWrongMsg);
                }
            } else {
                System.out.println("Not an AuthMessage received");
                ReferenceCountUtil.release(msg);
            }

        } else {
            System.out.println("Sending message further...");
            ctx.fireChannelRead(msg);
        }
    }
}
