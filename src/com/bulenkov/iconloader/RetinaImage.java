/*
 * Copyright 1998-2014 Konstantin Bulenkov
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

import com.bulenkov.iconloader.util.SystemInfo;
import com.bulenkov.iconloader.util.UIUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Konstantin Bulenkov
 */
public class RetinaImage {

  public static Image createFrom(Image image, final int scale, Component ourComponent) {
    final int w = image.getWidth(ourComponent);
    final int h = image.getHeight(ourComponent);

    final Image hidpi = create(image, w / scale, h / scale, BufferedImage.TYPE_INT_ARGB);

    if (SystemInfo.isAppleJvm) {
      Graphics2D g = (Graphics2D) hidpi.getGraphics();
      g.scale(1f / scale, 1f / scale);
      g.drawImage(image, 0, 0, null);
      g.dispose();
    }

    return hidpi;
  }

  public static BufferedImage create(int width, int height, int type) {
    return create(null, width, height, type);
  }


  private static BufferedImage create(Image image, int width, int height, int type) {
    if (SystemInfo.isAppleJvm) {
      return AppleHiDPIScaledImage.create(width, height, type);
    } else {
      if (image == null) {
        return new JBHiDPIScaledImage(width, height, type);
      } else {
        return new JBHiDPIScaledImage(image, width, height, type);
      }
    }
  }

  public static boolean isAppleHiDPIScaledImage(Image image) {
    return SystemInfo.isMac && UIUtil.isRetina() && AppleHiDPIScaledImage.is(image);
  }
}
