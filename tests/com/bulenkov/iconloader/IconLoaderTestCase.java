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

import com.bulenkov.iconloader.util.Ref;
import com.bulenkov.iconloader.util.UIUtil;
import junit.framework.TestCase;

import javax.swing.*;
import java.lang.reflect.Field;

/**
 * @author Konstantin Bulenkov
 */
public abstract class IconLoaderTestCase extends TestCase {
  static {
    IconLoader.enableSaveRealIconPath();
  }

  public static void setRetina(boolean isRetina) throws Exception {
    if (isRetina == UIUtil.isRetina()) {
      return;
    }

    final Field ourRetina = UIUtil.class.getDeclaredField("ourRetina");
    ourRetina.setAccessible(true);
    //noinspection unchecked
    final Ref<Boolean> ref = (Ref<Boolean>)ourRetina.get(null);
    ref.set(isRetina);
  }

  public static void setDarkIcons(boolean dark) throws Exception {
    IconLoader.setUseDarkIcons(dark);
  }

  public void checkIcon(String path, boolean isRetina, boolean isDark, String expectedName) throws Exception {
    setRetina(isRetina);
    setDarkIcons(isDark);
    final Icon icon = IconLoader.getIcon(path);
    assert icon != null : "Can't find icon '" + path + "'";
    icon.getIconHeight();

    final String realPath = ((IconLoader.CachedImageIcon) icon).myOriginalPath;
    assert realPath != null && realPath.endsWith(expectedName) :
        "Icon should be loaded from '" + expectedName + "' but it was loaded from '" + realPath + "'";
  }
}
