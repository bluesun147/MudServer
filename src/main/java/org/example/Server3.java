package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// https://cbw1030.tistory.com/51 - 소켓 양방향 통신

public class Server2 {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket socket = null;

        // 내보내는 데이터
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;

        // 들어오는 데이터
        InputStream inputStream = null;
        DataInputStream dataInputStream = null;

        try {
            serverSocket = new ServerSocket(9000);
            System.out.println("클라로부터 데이터 전송받을 준비 완료");

            // 클라가 들어올 때까지 기다렸다가 들어오면 accept 메서드가 받고 소켓에 저장
            socket = serverSocket.accept();
            System.out.println("클라이언트 연결 완료");
            System.out.println("socket" + socket);

            // 들어오는 받기 위해
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // 데이터 내보내기 위해
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            while (true) {
                // 1. 받기
                // 클라이어트가 보낸 (== 클라에서 들어오는 데이터)
                String clientMessage = dataInputStream.readUTF();
                System.out.println("clientMessage = " + clientMessage);
                
                // 2. 보내기
                // 클라로 내보내는 (out) 스트림
                dataOutputStream.writeUTF("메시지 전송 완료 : "+ clientMessage);
                dataOutputStream.flush(); // 전송했던 변수 완전히 비우기 위해서

                // 클라가 보낸 메시지가 stop이면 중지
                if (clientMessage.equals("stop")) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dataOutputStream != null) dataOutputStream.close();
                if (outputStream != null) outputStream.close();
                if (dataInputStream != null) dataInputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}