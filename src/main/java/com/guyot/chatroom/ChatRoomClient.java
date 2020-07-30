package com.guyot.chatroom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRoomClient {
    private Selector selector;
    private SocketChannel sc;

    public ChatRoomClient(int serverPort) {
        try {
            selector = Selector.open();
            sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(serverPort));
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_READ, ByteBuffer.wrap(new byte[500]));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void start() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new ReadMsgTask());
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String msg = scanner.nextLine();
            try {
                sc.write(ByteBuffer.wrap(msg.getBytes()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 读取服务端转发的信息。
     * 因为不能阻塞用户的（发送消息）的主线程，所以设计为异步
     */
    class ReadMsgTask implements Runnable {
        @Override
        public void run() {
                try {
                    while (selector.select() > 0) {
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            ByteBuffer buf = (ByteBuffer)key.attachment();
                            if (key.isReadable()) {
                                SocketChannel channel = (SocketChannel) key.channel();
                                buf.clear();
                                channel.read(buf);
                                System.out.println(new String(buf.array()));
                            }
                            iterator.remove();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }


    public static void main(String[] args) {
        new ChatRoomClient(7000).start();
    }


}
