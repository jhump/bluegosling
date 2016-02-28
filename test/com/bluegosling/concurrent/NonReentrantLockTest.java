package com.bluegosling.concurrent;

import java.util.concurrent.locks.Lock;


public class NonReentrantLockTest extends AbstractLockTest {

   @Override
   protected Lock makeLock() {
      return new NonReentrantLock();
   }

   @Override
   protected boolean isReentrant() {
      return false;
   }

}
