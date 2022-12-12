package org.example;

import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.io.PrintWriter;
import java.util.*;

public class Monster {

    private HashMap hm;
    private JSONObject jo;

    public Monster(HashMap hm, JSONObject jo) {
        this.hm = hm;
        this.jo = jo;
    }

    static JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT);
    static Jedis jedis = pool.getResource();


    // 유저 공격
    // 주위 9칸의 유저 공격 --> 유저의 attack이랑 같은 방법
    // but 모든 유저가 해야 하므로 몬스터 리스트 돌리면서 한마리씩 반복해야 함.
    public void attack() {
        Set<String> monsters = jedis.keys("monster:*:hp");
        Iterator<String> iter = monsters.iterator(); // set을 Iterator 안에 담기
        List<String> monsterList = new ArrayList<String>();
        while (iter.hasNext()) { // iterator에 다음 값이 있다면
            monsterList.add(iter.next().split(":")[1]); // 이름만 꺼내기
        }
        String[] monsterArr = monsterList.toArray(new String[monsterList.size()]); // 몬스터 이름 배열

        for (String monster : monsterArr) {

            // 몬스터 위치
            String monsterX = jedis.hget("monster:" + monster + ":space", "X");
            String monsterY = jedis.hget("monster:" + monster + ":space", "Y");

            String plusMessage = ""; // 추가로 보낼 메시지

            Set<String> users = jedis.keys("user:*:hp");
            Iterator<String> iterU = users.iterator(); // set을 Iterator 안에 담기
            List<String> userList = new ArrayList<String>();
            while (iterU.hasNext()) { // iterator에 다음 값이 있다면
                userList.add(iterU.next().split(":")[1]); // 이름만 꺼내기
            }
            String userArr[] = userList.toArray(new String[userList.size()]); // 유저 이름 배열

            for (String user : userArr) {
                Object obj = hm.get(user);
                if (obj != null) { // 몬스터가 접속한 유저만 공격

                    String userX = jedis.hget("user:" + user + ":space", "X");
                    String userY = jedis.hget("user:" + user + ":space", "Y");

                    int monsterStr = Integer.parseInt(jedis.get("monster:" + monster + ":str"));

                    int userXInt = Integer.parseInt(userX);
                    int userYInt = Integer.parseInt(userY);

                    // 몬스터의 9칸 범위내에 있는 유저가 있으면 체력 감소시킴
                    if (((userXInt == Integer.parseInt(monsterX)) || (userXInt == Integer.parseInt(monsterX) - 1) || (userXInt == Integer.parseInt(monsterX) + 1))
                            && ((userYInt == Integer.parseInt(monsterY)) || (userYInt == Integer.parseInt(monsterY) - 1) || (userYInt == Integer.parseInt(monsterY) + 1))) {
                        // 유저 있다면 체력 감소시킴
                        System.out.println("몬스터 : 공격!");
                        jedis.decrBy("user:" + user + ":hp", monsterStr);

                        String userHp = jedis.get("user:" + user + ":hp");
                        // 유저 사망
                        if (Integer.parseInt(userHp) <= 0) {
                            System.out.println(user + "유저 사망");

                            plusMessage += "\n" + user + " 사망!";

                            jo.put("fromServerJsonKey", monster + "이 " + user + "을 공격해서 데미지 " + monsterStr + "을 가했습니다. 유저 hp : " + userHp + plusMessage);
                            broadcast(jo.toString());

                            jedis.del("user:" + user); // 이름
                            jedis.del("user:" + user + ":hp"); // hp
                            jedis.del("user:" + user + ":str"); // str
                            jedis.hdel("user:" + user + ":space", "X");
                            jedis.hdel("user:" + user + ":space", "Y"); // 좌표
                            jedis.hdel("user:" + user + ":potions", "hpPotion");
                            jedis.hdel("user:" + user + ":potions", "strPotion"); // 포션 삭제

                        } else {
                            // 이미 죽어서 이름 삭제됐기 때문에 사망은 안뜨는거. 메시지 전송을 먼저 해야 함.
                            // 모든 클라에게 메시지 전송!!!!
                            ////// 이게 broadcast
                            jo.put("fromServerJsonKey", monster + "이 " + user + "을 공격해서 데미지 " + monsterStr + "을 가했습니다. 유저 hp : " + userHp + plusMessage);
                            broadcast(jo.toString());
                        }
                    }
                }
            }
        }
    }

    // 몬스터 몇마리인지 카운트
    public static int countMonster() {

        Set<String> keys = jedis.keys("monster:*:hp");
        return keys.size();
    }

    // 몬스터 생성
    public static void genMonster() {

        System.out.println("몬스터 생성");

        // (int) (Math.random() * (최댓값-최소값+1) + 최소값)
        // 위치 설정
        // 최대 29, 최소 0
        String monsterX = String.valueOf((int) (Math.random() * (29 - 0 + 1) + 0)); // 랜덤 x 좌표
        String monsterY = String.valueOf((int) (Math.random() * (29 - 0 + 1) + 0)); // 랜덤 y 좌표

        String uuid = UUID.randomUUID().toString();

        // 몬스터 생성
        jedis.set("monster:" + uuid, "1"); // 몬스터 등록

        // 임의 위치 배정
        jedis.hset("monster:" + uuid + ":space", "X", monsterX);
        jedis.hset("monster:" + uuid + ":space", "Y", monsterY);

        // 범위 내 랜덤 숫자 : (int) (Math.random() * (최댓값-최소값+1) + 최소값)

        // 임의 위치 배정
        // 임의 체력 설정 (5~10)
        String monsterHp = String.valueOf((int) (Math.random() * (10 - 5 + 1) + 5));
        jedis.set("monster:" + uuid + ":hp", monsterHp);
        // 임의 공격력 설정 (3~5)
        String monsterStr = String.valueOf((int) (Math.random() * (5 - 3 + 1) + 3));
        jedis.set("monster:" + uuid + ":str", monsterStr);

        // 몬스터가 포션 갖고 있을 지 랜덤 설정 (hp 포션 한번, str 포선 한번)
        Random random = new Random();
        Boolean hpPotionFlag = random.nextBoolean(); // true or false
        Boolean strPotionFlag = random.nextBoolean();
        if (hpPotionFlag) { // 체력 회복 포션
            jedis.hset("monster:" + uuid + ":potions", "hpPotion", "1");
        }
        if (strPotionFlag) { // 공격력 강화 포션
            jedis.hset("monster:" + uuid + ":potions", "strPotion", "1");
        }
    }

    // 몬스터 관리
    public void manageMonsterGenerate() {
        // 몬스터 체크 및 생성
        TimerTask countTask = new TimerTask() {
            @Override
            public void run() {
                while (Monster.countMonster() < 10) {
                    Monster.genMonster(); // 몬스터 생성
                    System.out.println("몬스터 마리 수 : " + Monster.countMonster()); // 몬스터 카운트
                }
                System.out.println("몬스터 마리 수 : " + Monster.countMonster()); // 몬스터 카운트
            }
        };

        // 60초마다 몬스터 카운트해서 10마리 될때까지 생성
        new Timer().scheduleAtFixedRate(countTask, 0L, 60 * 1000);
    }

    public void manageMonsterAttack() {

        TimerTask attackTask = new TimerTask() {
            @Override
            public void run() {
                attack();
            }
        };

        // 5초마다 몬스터들이 공격 실행
        new Timer().scheduleAtFixedRate(attackTask, 100L, 5 * 1000); // 딜레이 0으로 하면 에러 남.
    }

    public void broadcast(String msg) {
        synchronized (hm) {
            Collection collection = hm.values();
            Iterator iter = collection.iterator();
            while (iter.hasNext()) {
                PrintWriter pw = (PrintWriter) iter.next();
                pw.println(msg);
                pw.flush();
            }
        }
    }
}