package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
public class RedissonTest {
    @Autowired
    private RedissonClient redissonClient;

    /*############################### Data Struct ####################################*/
    @Test
    void testBucket() {
        RBucket<String> bucket = redissonClient.getBucket("key-test");
        bucket.set("value-test");
        bucket.expire(10, TimeUnit.SECONDS);
        System.out.println(bucket.get());
    }

    @Test
    void testMap() {

        RMap<String, String> map = redissonClient.getMap("key-map-test");
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        Object v1 = map.get("k1");
        log.debug("v1:{}", v1);
        map.clear();

    }

    @Test
    void testList() {
        RList<String> list = redissonClient.getList("key-list-test");
        list.add("v1");
        list.add("v2");
        list.add("v3");
        log.debug("list:{}", list);
        list.remove(0);
    }

    @Test
    void testSet() {
        RSet<String> set = redissonClient.getSet("key-set-test");
        set.add("v1");
        set.add("v2");
        set.add("v3");
        log.debug("set:{}", set);
        set.remove(0);
    }

    /*############################### Lock ####################################*/

    @Test
    void testLock() {
        RLock lock = redissonClient.getLock("key-lock-test");
        /**
         * 公平锁，多个线程在尝试获取同一把锁时，按照申请锁的先后顺序来获取锁。
         * 默认情况下，当一个线程获取锁之后，其他线程会等待，直到锁被释放。
         * 很有可能会发生死锁!死锁会中断程序并抛出异常
         * 公平锁会自动释放大概30s左右,
         */
        //加锁并设置生效时长50s
        lock.lock(50000, TimeUnit.MILLISECONDS);
        try {
            log.debug("加锁成功，执行业务...");
        } finally {
            lock.unlock();
        }
    }

    @Test
    void testWatchDog() {
        /**
         * 看门狗机制解决以下问题
         * 如果客户端持有锁的时间超过了锁的过渡时间，但业务尚未执行完成，锁会被自动释放，导致其他线程获取到锁。
         *
         */
        RLock lock = redissonClient.getLock("key-lock-test-watchdog");
        //不指定leaseTime，会自动启动看门狗：执行业务超过30s会自动重置锁的生效时间，保证锁一直存在，一直到业务执行完成！
        lock.lock();
        try {
            log.debug("加锁成功，执行业务...");
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 多重入锁。
     * 同一个线程，可n次获取同一把锁。只有释放了n次锁，才能将该锁完整释放或者30s后自动释放锁。
     */
    @Test
    void testMultipleEntryLock(){

        RLock lock = redissonClient.getLock("key-lock-test-multiple");
        lock.lock();
        try {
            log.debug("第一层加锁成功，执行业务...");
            TimeUnit.SECONDS.sleep(5);
            lock.lock();
            log.debug("第二层加锁成功，执行业务...");
            TimeUnit.SECONDS.sleep(5);
            lock.lock();
            log.debug("第三层加锁成功，执行业务...");
            TimeUnit.SECONDS.sleep(5);



        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            lock.unlock(); // key：keykey-lock-test-multiple  value：3
            lock.unlock(); // key：keykey-lock-test-multiple  value：2
            lock.unlock(); // key：keykey-lock-test-multiple  value：1
            log.debug("锁释放成功！");
        }

    }

    @Test
    void testTryLock() throws InterruptedException {
        RLock lock = redissonClient.getLock("key-lock-test-retry");
        // 尝试获取锁，最多等待5秒，最多持有10秒
        /**
         * PubSub机制：使用Redis的发布订阅，当锁释放时主动通知等待线程
         * 信号量控制：使用信号量实现线程的等待和唤醒
         * 避免空转：不是盲目循环，而是阻塞等待通知
         */
        boolean isLock = lock.tryLock(5, 100, TimeUnit.SECONDS);
        if (isLock) {
            try {
                log.debug("加锁成功，执行业务...");
            } finally {
                lock.unlock();
            }
        }
    }

    @Test
    void testReadLock() {
        /**
         * 读锁：多个线程可以同时获取读锁。
         */
        Thread t1 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-r-test");
            lock.readLock().lock();
            try {
                log.debug("t1:读锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.writeLock().unlock();
            }
        });

        Thread t2 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-rw-test");
            lock.readLock().lock();
            try {
                log.debug("t2:读锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.readLock().unlock();
            }
        });

        t1.start();
        t2.start();


    }

    @Test
    void testWriteLock() {
        /**
         * 写锁：只有一个线程可以获取写锁。
         * 写锁不会自动释放
         */
        Thread t1 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-w-test");
            lock.writeLock().lock();
            try {
                log.debug("t1:写锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.writeLock().unlock();
            }
        });

        Thread t2 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-w-test");
            lock.writeLock().lock();
            try {
                log.debug("t2:写锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.writeLock().unlock();
            }
        });

        t1.start();
        t2.start();
    }

    @Test
    void testRWLock() throws InterruptedException {
        /**
         * 读写锁：多个线程可以同时获取读锁，但是只有一个线程可以获取写锁。
         * 高并发：写锁和读锁不能同时存在！
         */

        Thread t1 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-rw-test");
            lock.readLock().lock(50, TimeUnit.SECONDS);
            try {
                log.debug("t1:读锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                log.debug("t1:读锁释放...");
                lock.readLock().unlock();
            }
        });


        Thread t2 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-rw-test");

            //最大锁等待时间15秒
            lock.writeLock().lock(50, TimeUnit.SECONDS);
            try {
                log.debug("t2:写锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                log.debug("t2:写锁释放...");
                lock.writeLock().unlock();
            }
        });

        Thread t3 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-rw-test");
            lock.readLock().lock(50, TimeUnit.SECONDS);
            try {
                log.debug("t3:读锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                log.debug("t3:读锁释放...");
                lock.readLock().unlock();
            }
        });

        Thread t4 = new Thread(() -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("key-rw-test");
            lock.writeLock().lock(50, TimeUnit.SECONDS);
            try {
                log.debug("t4:写锁加锁成功，执行业务...");
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                log.debug("t4:写锁释放...");
                lock.writeLock().unlock();
            }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();

        Thread.sleep(20000);

    }

    /**
     * MultiLock是Redisson实现的RedLock算法，用于解决单点Redis故障导致的多重锁定安全性问题。
     * 问题：如果Redis主节点宕机
     * 1. 从节点可能还没同步锁数据
     * 2. 从节点提升为主节点
     * 3. 其他客户端可能获取到同一把锁
     * 结果：锁的互斥性被破坏
     *
     * RedLock算法：
     * 1. 获取当前时间戳（毫秒）
     *    ↓
     * 2. 依次在N个Redis实例上尝试获取锁
     *    - 使用相同的key和随机值
     *    - 每个实例设置较短的超时时间
     *    ↓
     * 3. 计算获取锁的总耗时
     *    ↓
     * 4. 判断是否成功：
     *    - 至少在 N/2 + 1 个实例上成功
     *    - 总耗时 < 锁的有效时间
     *    ↓
     * 5. 如果成功：
     *    - 锁的实际有效时间 = 原有效时间 - 获取锁的总耗时
     *    ↓
     * 6. 如果失败：
     *    - 释放所有已获取的锁
     */
    @Test
    void testMultiLock() {

        RLock lock1 = redissonClient.getLock("key-multi-test");
        RLock lock2 = redissonClient.getLock("key-multi-test");
        RLock lock3 = redissonClient.getLock("key-multi-test");
        RLock multiLock = redissonClient.getMultiLock(lock1, lock2, lock3);
        multiLock.lock();
        try {
            log.debug("获取锁成功，执行业务...");
        } finally {
            multiLock.unlock();
        }
    }


}
