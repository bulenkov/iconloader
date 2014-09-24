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

/**
 * @author Konstantin Bulenkov
 */
public class IconLoaderTest extends IconLoaderTestCase {
  public void testSimple() throws Exception {
    checkIcon("/icons/print.png", false, false, "print.png");
  }

  public void testSimpleRetina() throws Exception {
    checkIcon("/icons/print.png", true, false, "print@2x.png");
  }

  public void testSimpleDark() throws Exception {
    checkIcon("/icons/print.png", false, true, "print_dark.png");
  }

  public void testSimpleRetinaAndDark() throws Exception {
    checkIcon("/icons/print.png", true, true, "print@2x_dark.png");
  }

  public void testNoRetinaIconAvailable() throws Exception {
    checkIcon("/icons/printPreview.png", true, false, "printPreview.png");
  }

  public void testNoDarkIconAvailable() throws Exception {
    checkIcon("/icons/printPreview.png", false, true, "printPreview.png");
  }

  public void testNoRetinaAndDarkIconAvailable() throws Exception {
    checkIcon("/icons/printPreview.png", true, true, "printPreview.png");
  }


}
