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

import com.bulenkov.iconloader.IsRetina;
import com.bulenkov.iconloader.JBHiDPIScaledImage;
import com.bulenkov.iconloader.RetinaImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.lang.reflect.Field;

/**
 * @author Konstantin Bulenkov
 */
public class UIUtil {
  public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

  public static <T extends JComponent> T findComponentOfType(JComponent parent, Class<T> cls) {
    if (parent == null || cls.isAssignableFrom(parent.getClass())) {
      @SuppressWarnings({"unchecked"}) final T t = (T) parent;
      return t;
    }
    for (Component component : parent.getComponents()) {
      if (component instanceof JComponent) {
        T comp = findComponentOfType((JComponent) component, cls);
        if (comp != null) return comp;
      }
    }
    return null;
  }

  public static <T> T getParentOfType(Class<? extends T> cls, Component c) {
    Component eachParent = c;
    while (eachParent != null) {
      if (cls.isAssignableFrom(eachParent.getClass())) {
        @SuppressWarnings({"unchecked"}) final T t = (T) eachParent;
        return t;
      }

      eachParent = eachParent.getParent();
    }

    return null;
  }


  public static Color getControlColor() {
    return UIManager.getColor("control");
  }

  public static Color getPanelBackground() {
    return UIManager.getColor("Panel.background");
  }

  public static boolean isUnderDarcula() {
    return UIManager.getLookAndFeel().getName().equals("Darcula");
  }

  public static Color getListBackground() {
    return UIManager.getColor("List.background");
  }

  public static Color getListForeground() {
    return UIManager.getColor("List.foreground");
  }

  public static Color getLabelForeground() {
    return UIManager.getColor("Label.foreground");
  }

  public static Color getTextFieldBackground() {
    return UIManager.getColor("TextField.background");
  }

  public static Color getTreeSelectionForeground() {
    return UIManager.getColor("Tree.selectionForeground");
  }

  public static Color getTreeForeground() {
    return UIManager.getColor("Tree.foreground");
  }

  private static final Color DECORATED_ROW_BG_COLOR = new DoubleColor(new Color(242, 245, 249), new Color(65, 69, 71));

  public static Color getDecoratedRowColor() {
    return DECORATED_ROW_BG_COLOR;
  }

  public static Color getTreeSelectionBackground(boolean focused) {
    return focused ? getTreeSelectionBackground() : getTreeUnfocusedSelectionBackground();
  }

  private static Color getTreeSelectionBackground() {
    return UIManager.getColor("Tree.selectionBackground");
  }

  public static Color getTreeUnfocusedSelectionBackground() {
    Color background = getTreeTextBackground();
    return ColorUtil.isDark(background) ? new DoubleColor(Gray._30, new Color(13, 41, 62)) : Gray._212;
  }

  public static Color getTreeTextBackground() {
    return UIManager.getColor("Tree.textBackground");
  }

  public static void drawImage(Graphics g, Image image, int x, int y, ImageObserver observer) {
    if (image instanceof JBHiDPIScaledImage) {
      final Graphics2D newG = (Graphics2D) g.create(x, y, image.getWidth(observer), image.getHeight(observer));
      newG.scale(0.5, 0.5);
      Image img = ((JBHiDPIScaledImage) image).getDelegate();
      if (img == null) {
        img = image;
      }
      newG.drawImage(img, 0, 0, observer);
      newG.scale(1, 1);
      newG.dispose();
    } else {
      g.drawImage(image, x, y, observer);
    }
  }


  private static final Ref<Boolean> ourRetina = Ref.create(SystemInfo.isMac ? null : false);

  public static boolean isRetina() {
    synchronized (ourRetina) {
      if (ourRetina.isNull()) {
        ourRetina.set(false); // in case HiDPIScaledImage.drawIntoImage is not called for some reason

        if (SystemInfo.isJavaVersionAtLeast("1.6.0_33") && SystemInfo.isAppleJvm) {
          if (!"false".equals(System.getProperty("ide.mac.retina"))) {
            ourRetina.set(IsRetina.isRetina());
            return ourRetina.get();
          }
        } else if (SystemInfo.isJavaVersionAtLeast("1.7.0_40") && SystemInfo.isOracleJvm) {
          GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
          final GraphicsDevice device = env.getDefaultScreenDevice();
          try {
            Field field = device.getClass().getDeclaredField("scale");
            if (field != null) {
              field.setAccessible(true);
              Object scale = field.get(device);
              if (scale instanceof Integer && (Integer) scale == 2) {
                ourRetina.set(true);
                return true;
              }
            }
          } catch (Exception ignore) {
          }
        }
        ourRetina.set(false);
      }

      return ourRetina.get();
    }
  }

  public static BufferedImage createImage(int width, int height, int type) {
    if (isRetina()) {
      return RetinaImage.create(width, height, type);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(width, height, type);
  }


  private static final GrayFilter DEFAULT_GRAY_FILTER = new GrayFilter(true, 65);
  private static final GrayFilter DARCULA_GRAY_FILTER = new GrayFilter(true, 30);

  public static GrayFilter getGrayFilter() {
    return isUnderDarcula() ? DARCULA_GRAY_FILTER : DEFAULT_GRAY_FILTER;
  }


}
