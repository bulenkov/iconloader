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

package com.bulenkov.iconloader;

import com.bulenkov.iconloader.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnusedDeclaration")
public final class IconLoader {
  public static boolean STRICT = false;
  private static boolean USE_DARK_ICONS = UIUtil.isUnderDarcula();

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final ConcurrentMap<URL, CachedImageIcon> ourIconsCache = new ConcurrentHashMap<URL, CachedImageIcon>(100, 0.9f, 2);

  /**
   * This cache contains mapping between icons and disabled icons.
   */
  private static final Map<Icon, Icon> ourIcon2DisabledIcon = new WeakHashMap<Icon, Icon>(200);


  private static final ImageIcon EMPTY_ICON = new ImageIcon(UIUtil.createImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)) {
    public String toString() {
      return "Empty icon " + super.toString();
    }
  };

  private static AtomicBoolean ourIsActivated = new AtomicBoolean(true);
  private static AtomicBoolean ourIsSaveRealIconPath = new AtomicBoolean(false);

  private IconLoader() {
  }

  public static final Component ourComponent = new Component() {
  };

  private static boolean waitForImage(Image image) {
    if (image == null) {
      return false;
    }

    if (image.getWidth(null) > 0) {
      return true;
    }

    MediaTracker mediatracker = new MediaTracker(ourComponent);
    mediatracker.addImage(image, 1);

    try {
      mediatracker.waitForID(1, 5000);
    } catch (InterruptedException ignore) {
    }

    return !mediatracker.isErrorID(1);
  }

  public static Pair<Image, String> loadFromUrl(URL url) {
    for (Pair<String, Integer> each : getFileNames(url.toString())) {
      try {
        return Pair.create(loadFromStream(URLUtil.openStream(new URL(each.first)), each.second), each.first);
      } catch (IOException ignore) {
      }
    }

    return null;
  }

  public static Image loadFromUrl(URL url, boolean dark, boolean retina) {
    for (Pair<String, Integer> each : getFileNames(url.toString(), dark, retina)) {
      try {
        return loadFromStream(URLUtil.openStream(new URL(each.first)), each.second);
      } catch (IOException ignore) {
      }
    }

    return null;
  }

  public static Image loadFromResource(String s) {
    int stackFrameCount = 2;
    Class callerClass = ReflectionUtil.findCallerClass(stackFrameCount);

    while (callerClass != null && callerClass.getClassLoader() == null) { // looks like a system class
      callerClass = ReflectionUtil.findCallerClass(++stackFrameCount);
    }

    if (callerClass == null) {
      callerClass = ReflectionUtil.findCallerClass(1);
    }

    if (callerClass == null) {
      return null;
    }

    return loadFromResource(s, callerClass);
  }

  public static Image loadFromResource(String path, Class aClass) {
    for (Pair<String, Integer> each : getFileNames(path)) {
      InputStream stream = aClass.getResourceAsStream(each.first);

      if (stream == null) continue;

      Image image = loadFromStream(stream, each.second);

      if (image != null) {
        return image;
      }
    }

    return null;
  }

  public static List<Pair<String, Integer>> getFileNames(String file) {
    return getFileNames(file, USE_DARK_ICONS, UIUtil.isRetina());
  }

  public static List<Pair<String, Integer>> getFileNames(String file, boolean dark, boolean retina) {
    if (retina || dark) {
      List<Pair<String, Integer>> answer = new ArrayList<Pair<String, Integer>>(4);

      final String name = StringUtil.getFileNameWithoutExtension(file);
      final String ext = StringUtil.getFileExtension(file);

      if (dark && retina) {
        answer.add(Pair.create(name + "@2x_dark." + ext, 2));
      }

      if (dark) {
        answer.add(Pair.create(name + "_dark." + ext, 1));
      }

      if (retina) {
        answer.add(Pair.create(name + "@2x." + ext, 2));
      }

      answer.add(Pair.create(file, 1));

      return answer;
    }

    return Collections.singletonList(Pair.create(file, 1));
  }

  public static Image loadFromStream(final InputStream inputStream) {
    return loadFromStream(inputStream, 1);
  }

  public static Image loadFromStream(final InputStream inputStream, final int scale) {
    if (scale <= 0) {
      throw new IllegalArgumentException("Scale must 1 or more");
    }

    try {
      BufferExposingByteArrayOutputStream outputStream = new BufferExposingByteArrayOutputStream();

      try {
        byte[] buffer = new byte[1024];

        while (true) {
          final int n = inputStream.read(buffer);
          if (n < 0) break;
          outputStream.write(buffer, 0, n);
        }

      } finally {
        inputStream.close();
      }

      Image image = Toolkit.getDefaultToolkit().createImage(outputStream.getInternalBuffer(), 0, outputStream.size());
      waitForImage(image);

      if (UIUtil.isRetina() && scale > 1) {
        image = RetinaImage.createFrom(image, scale, ourComponent);
      }

      return image;
    } catch (Exception ignore) {
    }

    return null;
  }


  private static Icon getIcon(final Image image) {
    return new MyImageIcon(image);
  }

  public static void setUseDarkIcons(boolean useDarkIcons) {
    USE_DARK_ICONS = useDarkIcons;
    ourIconsCache.clear();
    ourIcon2DisabledIcon.clear();
  }

  //TODO[kb] support iconsets
  //public static Icon getIcon(final String path, final String darkVariantPath) {
  //  return new InvariantIcon(getIcon(path), getIcon(darkVariantPath));
  //}

  public static Icon getIcon(final String path) {
    int stackFrameCount = 2;
    Class callerClass = ReflectionUtil.findCallerClass(stackFrameCount);

    while (callerClass != null && callerClass.getClassLoader() == null) { // looks like a system class
      callerClass = ReflectionUtil.findCallerClass(++stackFrameCount);
    }

    if (callerClass == null) {
      callerClass = ReflectionUtil.findCallerClass(1);
    }

    assert callerClass != null : path;

    return getIcon(path, callerClass);
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(java.lang.String)}
   */
  public static Icon findIcon(String path) {
    int stackFrameCount = 2;
    Class callerClass = ReflectionUtil.findCallerClass(stackFrameCount);
    while (callerClass != null && callerClass.getClassLoader() == null) { // looks like a system class
      callerClass = ReflectionUtil.findCallerClass(++stackFrameCount);
    }
    if (callerClass == null) {
      callerClass = ReflectionUtil.findCallerClass(1);
    }
    if (callerClass == null) return null;
    return findIcon(path, callerClass);
  }

  public static Icon getIcon(String path, final Class aClass) {
    final Icon icon = findIcon(path, aClass);
    assert icon != null : "Icon cannot be found in '" + path + "', aClass='" + aClass + "'";
    return icon;
  }

  public static void activate() {
    ourIsActivated.set(true);
  }

  public static void disable() {
    ourIsActivated.set(false);
  }

  public static boolean isLoaderDisabled() {
    return !ourIsActivated.get();
  }

  /**
   * This method is for test purposes only
   */
  static void enableSaveRealIconPath() {
    ourIsSaveRealIconPath.set(true);
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(java.lang.String, java.lang.Class)}
   */
  public static Icon findIcon(final String path, final Class aClass) {
    return findIcon(path, aClass, true);
  }

  public static Icon findIcon(String path, final Class aClass, boolean computeNow) {
    final URL myURL = aClass.getResource(path);

    if (myURL == null) {
      if (STRICT) {
        throw new RuntimeException("Can't find icon in '" + path + "' near " + aClass);
      }

      return null;
    }

    return findIcon(myURL);
  }

  public static Icon findIcon(URL url) {
    if (url == null) {
      return null;
    }

    CachedImageIcon icon = ourIconsCache.get(url);

    if (icon == null) {
      icon = new CachedImageIcon(url);
      icon = ConcurrencyUtil.cacheOrGet(ourIconsCache, url, icon);
    }

    return icon;
  }

  public static Icon findIcon(String path, ClassLoader classLoader) {
    if (!StringUtil.startsWithChar(path, '/')) {
      return null;
    }

    final URL url = classLoader.getResource(path.substring(1));
    return findIcon(url);
  }

  private static Icon checkIcon(final Image image, URL url) {
    if (image == null || image.getHeight(LabelHolder.ourFakeComponent) < 1) {
      // image wasn't loaded or broken
      return null;
    }

    final Icon icon = getIcon(image);
    if (icon != null && !isGoodSize(icon)) {
      return EMPTY_ICON;
    }

    return icon;
  }

  public static boolean isGoodSize(final Icon icon) {
    return icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
  }

  /**
   * Gets (creates if necessary) disabled icon based on the passed one.
   *
   * @return <code>ImageIcon</code> constructed from disabled image of passed icon.
   */
  public static Icon getDisabledIcon(Icon icon) {
    if (icon instanceof LazyIcon) {
      icon = ((LazyIcon) icon).getOrComputeIcon();
    }

    if (icon == null) {
      return null;
    }

    Icon disabledIcon = ourIcon2DisabledIcon.get(icon);
    if (disabledIcon == null) {
      if (!isGoodSize(icon)) {
        return EMPTY_ICON;
      }

      final int scale = UIUtil.isRetina() ? 2 : 1;
      final BufferedImage image = new BufferedImage(scale * icon.getIconWidth(), scale * icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

      final Graphics2D graphics = image.createGraphics();

      graphics.setColor(UIUtil.TRANSPARENT_COLOR);
      graphics.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
      graphics.scale(scale, scale);
      icon.paintIcon(LabelHolder.ourFakeComponent, graphics, 0, 0);

      graphics.dispose();

      Image img = createDisabled(image);

      if (UIUtil.isRetina()) {
        img = RetinaImage.createFrom(img, 2, ourComponent);
      }

      disabledIcon = new MyImageIcon(img);
      ourIcon2DisabledIcon.put(icon, disabledIcon);
    }

    return disabledIcon;
  }

  private static Image createDisabled(BufferedImage image) {
    final GrayFilter filter = UIUtil.getGrayFilter();
    final ImageProducer prod = new FilteredImageSource(image.getSource(), filter);
    return Toolkit.getDefaultToolkit().createImage(prod);
  }

  public static Icon getTransparentIcon(final Icon icon) {
    return getTransparentIcon(icon, 0.5f);
  }

  public static Icon getTransparentIcon(final Icon icon, final float alpha) {
    return new Icon() {
      @Override
      public int getIconHeight() {
        return icon.getIconHeight();
      }

      @Override
      public int getIconWidth() {
        return icon.getIconWidth();
      }

      @Override
      public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        final Graphics2D g2 = (Graphics2D) g;
        final Composite saveComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        icon.paintIcon(c, g2, x, y);
        g2.setComposite(saveComposite);
      }
    };
  }

  static final class CachedImageIcon implements Icon {
    private Object myRealIcon;
    String realPath;

    private final URL myUrl;
    private boolean dark;

    public CachedImageIcon(URL url) {
      myUrl = url;
      dark = USE_DARK_ICONS;
    }

    private synchronized Icon getRealIcon() {
      if (isLoaderDisabled()) {
        return EMPTY_ICON;
      }

      if (dark != USE_DARK_ICONS) {
        myRealIcon = null;
        dark = USE_DARK_ICONS;
      }

      Object realIcon = myRealIcon;

      if (realIcon instanceof Icon) {
        return (Icon) realIcon;
      }

      Icon icon;

      if (realIcon instanceof Reference) {
        icon = (Icon) ((Reference) realIcon).get();
        if (icon != null) {
          return icon;
        }
      }

      Pair<Image, String> image = loadFromUrl(myUrl);
      icon = image != null ? checkIcon(image.first, myUrl) : null;

      if (icon != null) {
        if (icon.getIconWidth() < 50 && icon.getIconHeight() < 50) {
          realIcon = icon;
        } else {
          realIcon = new SoftReference<Icon>(icon);
        }

        myRealIcon = realIcon;

        if (ourIsSaveRealIconPath.get()) {
          realPath = image.second;
        }
      }

      return icon == null ? EMPTY_ICON : icon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      getRealIcon().paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return getRealIcon().getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return getRealIcon().getIconHeight();
    }

    @Override
    public String toString() {
      return myUrl.toString();
    }
  }

  private static final class MyImageIcon extends ImageIcon {
    public MyImageIcon(final Image image) {
      super(image);
    }

    @Override
    public final synchronized void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final ImageObserver observer = getImageObserver();
      UIUtil.drawImage(g, getImage(), x, y, observer == null ? c : observer);
    }
  }

  public abstract static class LazyIcon implements Icon {
    private boolean myWasComputed;
    private Icon myIcon;
    private boolean isDarkVariant = USE_DARK_ICONS;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      final Icon icon = getOrComputeIcon();

      if (icon != null) {
        icon.paintIcon(c, g, x, y);
      }
    }

    @Override
    public int getIconWidth() {
      final Icon icon = getOrComputeIcon();
      return icon != null ? icon.getIconWidth() : 0;
    }

    @Override
    public int getIconHeight() {
      final Icon icon = getOrComputeIcon();
      return icon != null ? icon.getIconHeight() : 0;
    }

    protected final synchronized Icon getOrComputeIcon() {
      if (!myWasComputed || isDarkVariant != USE_DARK_ICONS) {
        isDarkVariant = USE_DARK_ICONS;
        myWasComputed = true;
        myIcon = compute();
      }

      return myIcon;
    }

    public final void load() {
      getIconWidth();
    }

    protected abstract Icon compute();
  }

  private static class LabelHolder {
    /**
     * To get disabled icon with paint it into the image. Some icons require
     * not null component to paint.
     */
    private static final JComponent ourFakeComponent = new JLabel();
  }
}
