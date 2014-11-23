package com.apriori.concurrent;

import java.util.concurrent.locks.Lock;


public class SpinLockTest extends AbstractLockTest {
   @Override
   protected Lock makeLock() {
      return new SpinLock();
   }
   
   @Override
   protected boolean isReentrant() {
      return false;
   }

}
