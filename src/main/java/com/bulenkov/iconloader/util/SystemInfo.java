/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bulenkov.iconloader.util;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings({"HardCodedStringLiteral", "UtilityClassWithoutPrivateConstructor", "UnusedDeclaration"})
public class SystemInfo {
  public static final String OS_NAME = System.getProperty("os.name");
  public static final String OS_VERSION = System.getProperty("os.version").toLowerCase();
  public static final String JAVA_VERSION = System.getProperty("java.version");
  public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");

  protected static final String _OS_NAME = OS_NAME.toLowerCase();
  public static final boolean isWindows = _OS_NAME.startsWith("windows");
  public static final boolean isOS2 = _OS_NAME.startsWith("os/2") || _OS_NAME.startsWith("os2");
  public static final boolean isMac = _OS_NAME.startsWith("mac");
  public static final boolean isLinux = _OS_NAME.startsWith("linux");
  public static final boolean isUnix = !isWindows && !isOS2;

  public static final boolean isFileSystemCaseSensitive = isUnix && !isMac;
  public static final boolean isMacOSLion = isLion();

  public static final boolean isAppleJvm = isAppleJvm();
  public static final boolean isOracleJvm = isOracleJvm();
  public static final boolean isSunJvm = isSunJvm();


  public static boolean isOsVersionAtLeast(String version) {
    return compareVersionNumbers(OS_VERSION, version) >= 0;
  }


  private static boolean isTiger() {
    return isMac &&
        !OS_VERSION.startsWith("10.0") &&
        !OS_VERSION.startsWith("10.1") &&
        !OS_VERSION.startsWith("10.2") &&
        !OS_VERSION.startsWith("10.3");
  }

  private static boolean isLeopard() {
    return isMac && isTiger() && !OS_VERSION.startsWith("10.4");
  }

  private static boolean isSnowLeopard() {
    return isMac && isLeopard() && !OS_VERSION.startsWith("10.5");
  }

  private static boolean isLion() {
    return isMac && isSnowLeopard() && !OS_VERSION.startsWith("10.6");
  }

  private static boolean isMountainLion() {
    return isMac && isLion() && !OS_VERSION.startsWith("10.7");
  }

  public static int compareVersionNumbers(String v1, String v2) {
    if (v1 == null && v2 == null) {
      return 0;
    }
    if (v1 == null) {
      return -1;
    }
    if (v2 == null) {
      return 1;
    }

    String[] part1 = v1.split("[\\.\\_\\-]");
    String[] part2 = v2.split("[\\.\\_\\-]");

    int idx = 0;
    for (; idx < part1.length && idx < part2.length; idx++) {
      String p1 = part1[idx];
      String p2 = part2[idx];

      int cmp;
      if (p1.matches("\\d+") && p2.matches("\\d+")) {
        cmp = new Integer(p1).compareTo(new Integer(p2));
      } else {
        cmp = part1[idx].compareTo(part2[idx]);
      }
      if (cmp != 0) return cmp;
    }

    if (part1.length == part2.length) {
      return 0;
    } else if (part1.length > idx) {
      return 1;
    } else {
      return -1;
    }
  }

  public static boolean isJavaVersionAtLeast(String v) {
    return StringUtil.compareVersionNumbers(JAVA_RUNTIME_VERSION, v) >= 0;
  }

  private static boolean isOracleJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "Oracle");
  }

  private static boolean isSunJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "Sun") && StringUtil.containsIgnoreCase(vendor, "Microsystems");
  }

  private static boolean isAppleJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "Apple");
  }

  public static String getJavaVmVendor() {
    return System.getProperty("java.vm.vendor");
  }


}
