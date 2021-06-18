package com.example.filesystem.client;

import com.example.filesystem.service.CommandHandle;
import com.example.filesystem.model.User;
import com.example.filesystem.util.UserLoginUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class Client {


    public static String ADDRESS_DEFAULT = "127.0.0.1";

    public static int PORT_DEFAULT = 9023;


    public static void main(String[] args) {

        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(ADDRESS_DEFAULT,PORT_DEFAULT));

            while (true){
                selector.select();
                Set<SelectionKey> keySet = selector.selectedKeys();

                for (SelectionKey selectionKey : keySet) {
                    if (selectionKey.isConnectable()) {

                        SocketChannel client = (SocketChannel) selectionKey.channel();

                        if (client.isConnectionPending()) {
                            client.finishConnect();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);


                            ExecutorService executorService = newSingleThreadExecutor(Executors.defaultThreadFactory());
                            executorService.submit(() ->{
                                while (true) {
                                    try {
                                        if (UserLoginUtil.loginMap.isEmpty()) {

                                            User user = UserLoginUtil.userLogin(byteBuffer, client);
                                            UserLoginUtil.loginMap.put(client, user);
                                        }else {
                                            byteBuffer.clear();
                                            InputStreamReader input = new InputStreamReader(System.in);
                                            BufferedReader reader = new BufferedReader(input);
                                            String sendMessage = reader.readLine();
                                            byteBuffer.put(sendMessage.getBytes());
                                            byteBuffer.flip();
                                            client.write(byteBuffer);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        client.register(selector, SelectionKey.OP_READ);
                    } else if (selectionKey.isReadable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        int count = client.read(readBuffer);
                        if (count > 0) {
                            String receiveMessage = new String(readBuffer.array(), 0, count);

                            CommandHandle.clientMessage(receiveMessage, client);
                        }
                    }
                }
                keySet.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
