///*
//package org.example;
//
//import org.json.JSONObject;
//import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.Protocol;
//
//import java.io.*;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//// https://blog.naver.com/duddnddl9/220614912095
//
//// https://cbw1030.tistory.com/51 - 소켓 단방향, 양방향 통신
//
//public class Server {
//    private static ExecutorService threadPool = Executors.newFixedThreadPool(10); // 스레드풀 10개 생성
//
//    public static void main(String[] args) {
//        ServerSocket serverSocket = null;
//        try {
//            // 서버 소켓 생성 및 포트 바인딩
//            serverSocket = new ServerSocket();
//            serverSocket.bind(new InetSocketAddress("localhost", 50001));
//
//            while (true) {
//                // 연결 수락 작업
//                System.out.println("[ 연결 기다림 ]");
//                Socket socket = serverSocket.accept();
//
//                // 원격에 있는 소켓 주소 얻겠다
//                InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
//                // 클라이언트 주소 얻기
//                String clientIp = isa.getHostName();
//                // System.out.println("Client IP: " + clientIp);
//
//                // 통신 작업 시작
//                // 위에서 선언한 10개의 스레드 중 하나가 아래의 task를 처리함
//                threadPool.submit(new Task(socket));
//
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (!serverSocket.isClosed()) {
//            try {
//                serverSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static class Task implements Runnable {
//        private Socket socket;
//
//        public Task(Socket socket) {
//            super();
//            this.socket = socket;
//        }
//
//        public void run() {
//            try {
//                InputStream is = socket.getInputStream();
//                Reader reader = new InputStreamReader(is, "EUC-KR");
//
//                BufferedReader br = new BufferedReader(reader);
//                String data = br.readLine();
//
//                JSONObject jsonRoot = new JSONObject(data);
//                String action = jsonRoot.getString("action");
//
//                JSONObject jsonData = jsonRoot.getJSONObject("data");
//
//                String mname = jsonData.getString("mname");
//                String mpassword = jsonData.getString("mpassword");
//
//                try (var pool = new JedisPool("localhost", Protocol.DEFAULT_PORT)) {
//                    try (var jedis = pool.getResource()) {
//
//                        String monsterX = "5"; // 몬스터 초기 x 좌표
//                        String monsterY = "10"; // 몬스터 초기 y 좌표
//
//                        String uuid = UUID.randomUUID().toString();
//
//                        // 몬스터 생성
//                        jedis.set("monster:monster" + uuid, "1"); // 몬스터 등록
//
//                        jedis.hset("monster:monster" + uuid + ":space", "X", monsterX);
//                        jedis.hset("monster:monster" + uuid + ":space", "Y", monsterY);
//
//                        // (int) (Math.random() * (최댓값-최소값+1) + 최소값)
//
//                        // 임의 위치 배정
//                        // 임의 체력 설정 (5~10)
//                        String monsterHp = String.valueOf((int) (Math.random() * (10 - 5 + 1) + 5));
//                        jedis.set("monster:monster" + uuid + ":hp", monsterHp);
//                        // 임의 공격력 설정 (3~5)
//                        String monsterStr = String.valueOf((int) (Math.random() * (5 - 3 + 1) + 3));
//                        jedis.set("monster:monster" + uuid + ":str", monsterStr);
//
//                        // 몬스터가 포션 갖고 있을 지 랜덤 설정 (hp 포션 한번, str 포선 한번)
//                        Random random = new Random();
//                        Boolean hpPotionFlag = random.nextBoolean();
//                        Boolean strPotionFlag = random.nextBoolean();
//                        if (hpPotionFlag) { // 체력 회복 포션
//                            jedis.hset("monster:monster" + uuid + ":potions", "hpPotion", "1");
//                        } if (strPotionFlag) { // 공격력 강화 포션
//                            jedis.hset("monster:monster" + uuid + ":potions", "strPotion", "1");
//                        }
//
//                        // 서버 -> 클라이언트
//                        String result = "";
//
//                        // jsonToClient은 클라로 다시 보내는 JSON 객체
//                        JSONObject jsonToClient = new JSONObject();
//
//                        String hp, str; // 체력, 공격력
//                        String userX, userY; // 사용자 위치
//
//                        // 이미 있는 유저라면
//                        if ("true".equals(jedis.get("user:" + mname))) {
//
//                            jsonToClient.put("action", "joinReject");
//                            System.out.println("이미 로그인 한 상태입니다.");
//                            userX = jedis.hget("user:" + mname + ":space", "X");
//                            userY = jedis.hget("user:" + mname + ":space", "Y");
//
//                            hp = jedis.get("user:" + mname + ":hp");
//                            str = jedis.get("user:" + mname + ":str");
//
//                            jsonToClient.put("data", "이미 로그인 한 상태입니다.\n" + "<<내 정보>>\n" + "x좌표: " + userX + ", " + "y좌표: " + userY + "\n" + "hp: " + hp + ", str: " + str);
//
//                        } else { // 신규 유저라면 초기 설정
//
//                            jedis.set("user:" + mname, "1"); // 유저 등록
//                            // (int) (Math.random() * (최댓값-최소값+1) + 최소값)
//                            // 최대 29, 최소 0
//                            userX = String.valueOf((int) (Math.random() * (29 - 0 + 1) + 0)); // 랜덤 x 좌표
//                            userY = String.valueOf((int) (Math.random() * (29 - 0 + 1) + 0)); // 랜덤 y 좌표
//                            System.out.println("X = " + userX);
//                            System.out.println("Y = " + userY);
//
//                            // 임의 위치 배정
//                            jedis.hset("user:" + mname + ":space", "X", userX);
//                            jedis.hset("user:" + mname + ":space", "Y", userY);
//
//                            // 체력 설정
//                            hp = "30";
//                            jedis.set("user:" + mname + ":hp", hp);
//                            // 공격력 설정
//                            str = "3";
//                            jedis.set("user:" + mname + ":str", str);
//
//                            // 포션 -> hset
//                            // 체력 회복 포션
//                            jedis.hset("user:" + mname + ":potions", "hpPotion", "1");
//                            // 공격력 강화 포션
//                            jedis.hset("user:" + mname + ":potions", "strPotion", "1");
//
//                            jsonToClient.put("action", "joinSuccess");
//                            System.out.println("회원가입 성공!");
//                            jsonToClient.put("data",
//                                    "회원가입 성공!\n" + "<<내 정보>>\n" + "x좌표: " + userX + ", " + "y좌표: " + userY + "\n" + "hp: " + hp + ", str: " + str);
//                        }
//
//                        // 몬스터 위치 정보
//                        jsonToClient.put("monsterSpace", "<<몬스터 위치>>\nx좌표: " + monsterX + ", " + "y좌표: " + monsterY);
//
//                        // 내 위치 정보
//                        jsonToClient.put("mySpace", "<<" + mname + " 위치>>\nx좌표: " + userX + ", " + "y좌표: " + userY);
//
//                        result = jsonToClient.toString();
//
//                        System.out.println("action : " + action);
//                        System.out.println("mname : " + mname);
//                        System.out.println("mpassword : " + mpassword);
//
//                        OutputStream os = socket.getOutputStream();
//                        PrintWriter pw = new PrintWriter(os);
//                        pw.println(result);
//                        pw.flush();
//                        os.flush();
//                        os.close();
//                        is.close();
//                        socket.close();
//                        System.out.println(Thread.currentThread().getName() + "가 처리함.");
//                    }
//                }
//
//
//            } catch (Exception e) {
//
//            }
//        }
//    }
//}*/


//package org.example;
//
//
//import java.net.ServerSocket;
//import java.net.Socket;
//
//public class Server {
//    public static void main(String[] args) {
//        ServerSocket serverSocket = null;
//        Socket socket = null;
//
//        try {
//            serverSocket = new ServerSocket(9000);
//            System.out.println("클라 받을 준비 완료");
//
//            socket = serverSocket.accept();
//            System.out.println("클라 연결!");
//            System.out.println("socket: " + socket);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                socket.close();
//                serverSocket.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//}

package org.example;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket socket = null;

        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;

        InputStream inputStream = null;
        DataInputStream dataInputStream = null;

        try {
            serverSocket = new ServerSocket(9000);
            System.out.println("클라로부터 데이터 전송받을 준비 완료");

            // 클라가 들어올 때까지 기다렸다가 들어오면 accept 메서드가 방고 소켓에 저장
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
                // 클라이어트가 보낸 (== 클라에서 들어오는 데이터)
                String clientMessage = dataInputStream.readUTF();
                System.out.println("clientMessage = " + clientMessage);


                // 클라로 내보내는 (out) 스트림
                dataOutputStream.writeUTF("메시지 전송 완료 : "+ clientMessage);
                dataOutputStream.flush(); // 완전히 삭제 위해.

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