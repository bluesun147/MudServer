package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

// https://wakestand.tistory.com/167

public class chatServer extends Thread {
    static ArrayList<Socket> list = new ArrayList<Socket>(); // 유저 확인용
    static Socket socket = null;

    public chatServer(Socket socket) {
        this.socket = socket; // 유저 socket을 할당
        list.add(socket); // 유저를 list에 추가
    }

    // 생성자에서 파라미터로 넘긴 소켓 정보 할당해준 뒤
    // list 안에 소켓 정보 add 시켜준 다음 run()으로 넘어감


    // Thread 에서 start() 메소드 사용 시 자동으로 해당 메소드 시작 (Thread별로 개별적 수행)
    public void run() {
        try {
            // 연결 확인용
            System.out.println("서버 : " + socket.getInetAddress() + " IP의 클라이언트와 연결되었습니다");

            // InputStream - 클라이언트에서 보낸 메세지 읽기
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            // OutputStream - 서버에서 클라이언트로 메세지 보내기
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);

            // 클라이언트에게 연결되었다는 메세지 보내기
            writer.println("서버에 연결되었습니다! ID를 입력해 주세요!");

            String readValue; // Client에서 보낸 값 저장
            String name = null; // 클라이언트 이름 설정용
            boolean identify = false;

            // while문으로 클라가 보낸 값 받으면서
            // 값 보냈을 때는 최초에 이름 할당해주고 넘김
            // 이후에 또 넘기면 for문 돌리면서 list안에 모든 유저 소켓 정보를 통해 메시지 보냄

            // 클라이언트가 메세지 입력시마다 수행
            while((readValue = reader.readLine()) != null ) {
                if(!identify) { // 연결 후 한번만 노출, 사용자 이름 입력
                    name = readValue; // 이름 할당
                    identify = true;
                    writer.println(name + "님이 접속하셨습니다.");
                    System.out.println(name + "님이 접속하셨습니다.");
                    continue;
                }

                // list 안에 클라이언트 정보가 담겨있음
                for(int i = 0; i<list.size(); i++) {
                    out = list.get(i).getOutputStream(); // 반복문 돌리면서 접속한 모든 클라에게 보냄
                    writer = new PrintWriter(out, true); // flush는 버퍼에 저장되어 있는 데이터 강제적으로 출력시킴.
                    // 클라이언트에게 메세지 발송
                    writer.println(name + " : " + readValue);
                    // 서버에도 기록
                }
                System.out.println(name + " : " + readValue); // 사용자 3명 접속 시 3번 찍힘
            }
        } catch (Exception e) {
            e.printStackTrace(); // 예외처리
        }
    }


    // 시작 시 포트 설정하고 서버 소켓 만든 뒤 while문 안에서
    // 클라 접속 시 socket정보 받음 -> 쓰레에 클라의 소켓 정보 넣고 start

    public static void main(String[] args) {
        try {
            int socketPort = 1234; // 소켓 포트 설정용
            ServerSocket serverSocket = new ServerSocket(socketPort); // 서버 소켓 만들기
            // 서버 오픈 확인용
            System.out.println("socket : " + socketPort + "으로 서버가 열렸습니다");

            // 소켓 서버가 종료될 때까지 무한루프
            while(true) {
                Socket socketUser = serverSocket.accept(); // 서버에 클라이언트 접속 시
                // Thread 안에 클라이언트 정보를 담아줌
                Thread thd = new chatServer(socketUser);
                thd.start(); // Thread 시작
            }

        } catch (IOException e) {
            e.printStackTrace(); // 예외처리
        }

    }

}