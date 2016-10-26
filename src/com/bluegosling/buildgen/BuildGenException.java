package com.bluegosling.buildgen;

public class BuildGenException extends RuntimeException {
   private static final long serialVersionUID = 7763185186430125779L;

   BuildGenException(String message) {
      super(message);
   }

   BuildGenException(String messageFormat, Object... formatArgs) {
      super(String.format(messageFormat, formatArgs));
   }
}
