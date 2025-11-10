package com.hmdp.inter;

public interface ILock {

    public boolean tryLock();

    public boolean unlock();
}
