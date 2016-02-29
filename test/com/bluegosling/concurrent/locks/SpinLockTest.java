package com.bluegosling.concurrent.locks;

import com.bluegosling.concurrent.locks.SpinLock;

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
