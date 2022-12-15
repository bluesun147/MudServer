package org.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

public class User {

    static JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT);
    static Jedis jedis = pool.getResource();

    public static void createUser(String name) {
        // 유저 등록
        jedis.set("user:" + name, "1");

        // 초기 설정

        // (int) (Math.random() * (최댓값-최소값+1) + 최소값)
        // 위치 설정
        // 최대 29, 최소 0
        String userX = String.valueOf((int) (Math.random() * (29 - 0 + 1) + 0)); // 랜덤 x 좌표
        String userY = String.valueOf((int) (Math.random() * (29 - 0 + 1) + 0)); // 랜덤 y 좌표

        // 임의 위치 배정
        jedis.hset("user:" + name + ":space", "X", userX);
        jedis.hset("user:" + name + ":space", "Y", userY);

        // 체력 설정
        String hp = "30";
        jedis.set("user:" + name + ":hp", hp);
        // 공격력 설정
        String str = "3";
        jedis.set("user:" + name + ":str", str);

        // 포션 -> hset
        // 체력 회복 포션
        jedis.hset("user:" + name + ":potions", "hpPotion", "1");
        // 공격력 강화 포션
        jedis.hset("user:" + name + ":potions", "strPotion", "1");

    }

    public static void deleteUser(String name) {
        jedis.del("user:" + name); // 이름
        jedis.del("user:" + name + ":hp"); // hp
        jedis.del("user:" + name + ":str"); // str
        jedis.hdel("user:" + name + ":space", "X");
        jedis.hdel("user:" + name + ":space", "Y"); // 좌표
        jedis.hdel("user:" + name + ":potions", "hpPotion");
        jedis.hdel("user:" + name + ":potions", "strPotion"); // 포션 삭제
    }
}