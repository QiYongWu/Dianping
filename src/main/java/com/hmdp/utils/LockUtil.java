package com.hmdp.utils;

import com.hmdp.inter.ILock;
import com.hmdp.inter.impl.ILockImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LockUtil {

    public boolean tryLockLoop(ILockImpl iLock, int loopTimes, long sleepMills) {
        for (int i = 0; i < loopTimes; i++) {
            if (iLock.tryLock()) {
                return true;
            }
            try {
                Thread.sleep(sleepMills);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
