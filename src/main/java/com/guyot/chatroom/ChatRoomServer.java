package com.guyot.chatroom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 群聊系统服务端
 * <p>
 * 支持功能：（三个功能在转发时需要排除自己）
 * 1.客户端上线时打印日志并转发消息
 * 2.客户端发送消息时转发消息
 * 3.客户端退出时转发消息
 */
public class ChatRoomServer {

    private int port;
    private Selector selector;
    private ServerSocketChannel ssc;

    public ChatRoomServer(int port) {
        this.port = port;
        try {
            selector = Selector.open();
            ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(port));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void start() {
        try {
            while (true) {
                if (selector.selectNow() <= 0) {
                    continue;
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        SocketChannel sc = ssc.accept();
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ, ByteBuffer.wrap(new byte[500]));
                        String msg = sc.getRemoteAddress().toString() + " connected";
                        System.out.println(msg);
                        multiPastMsg(msg, sc);
                    } else if (key.isReadable()) {
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        buffer.clear();
                        SocketChannel sc = (SocketChannel) key.channel();
                        sc.read(buffer);
                        String msg = "From " + sc.getRemoteAddress().toString() + ": " + new String(buffer.array());
                        System.out.println(msg);
                        multiPastMsg(msg, sc);
                    } else {
                        //NOOP
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //转发消息给其他客户端
    private void multiPastMsg(String msg, SocketChannel self) {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            Channel channel = key.channel();
            if (channel instanceof SocketChannel && channel != self) {
                try {
                    ((SocketChannel) channel).write(ByteBuffer.wrap(msg.getBytes()));
                } catch (IOException e) {
                    //转发时发生异常，此时如果是以为客户端下线，需要打印日志（并转发到其他在线的客户端）
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        new ChatRoomServer(7000).start();
    }

}
