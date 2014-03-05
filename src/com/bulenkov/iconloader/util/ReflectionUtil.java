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

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class ReflectionUtil {

  private ReflectionUtil() {
  }


  public static Type resolveVariable(TypeVariable variable, Class classType) {
    return resolveVariable(variable, classType, true);
  }


  public static Type resolveVariable(TypeVariable variable, Class classType, boolean resolveInInterfacesOnly) {
    final Class aClass = getRawType(classType);
    int index = ArrayUtilRt.find(aClass.getTypeParameters(), variable);

    if (index >= 0) {
      return variable;
    }

    final Class[] classes = aClass.getInterfaces();
    final Type[] genericInterfaces = aClass.getGenericInterfaces();

    for (int i = 0; i <= classes.length; i++) {
      Class anInterface;

      if (i < classes.length) {
        anInterface = classes[i];
      } else {
        anInterface = aClass.getSuperclass();
        if (resolveInInterfacesOnly || anInterface == null) {
          continue;
        }
      }

      final Type resolved = resolveVariable(variable, anInterface);

      if (resolved instanceof Class || resolved instanceof ParameterizedType) {
        return resolved;
      }

      if (resolved instanceof TypeVariable) {
        final TypeVariable typeVariable = (TypeVariable) resolved;
        index = ArrayUtilRt.find(anInterface.getTypeParameters(), typeVariable);

        assert index >= 0 : "Cannot resolve type variable:\n"
            + "typeVariable = " + typeVariable + "\n"
            + "genericDeclaration = " + declarationToString(typeVariable.getGenericDeclaration()) + "\n"
            + "searching in " + declarationToString(anInterface);

        final Type type = i < genericInterfaces.length ? genericInterfaces[i] : aClass.getGenericSuperclass();

        if (type instanceof Class) {
          return Object.class;
        }

        if (type instanceof ParameterizedType) {
          return getActualTypeArguments((ParameterizedType) type)[index];
        }

        throw new AssertionError("Invalid type: " + type);
      }
    }

    return null;
  }


  public static String declarationToString(GenericDeclaration anInterface) {
    return anInterface.toString()
        + Arrays.asList(anInterface.getTypeParameters())
        + " loaded by " + ((Class) anInterface).getClassLoader();
  }

  public static Class<?> getRawType(Type type) {
    if (type instanceof Class) {
      return (Class) type;
    }

    if (type instanceof ParameterizedType) {
      return getRawType(((ParameterizedType) type).getRawType());
    }

    if (type instanceof GenericArrayType) {
      //todo[kb] don't create new instance each time
      return Array.newInstance(getRawType(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
    }

    assert false : type;

    return null;
  }

  public static Type[] getActualTypeArguments(ParameterizedType parameterizedType) {
    return parameterizedType.getActualTypeArguments();
  }

  public static Class<?> substituteGenericType(Type genericType, Type classType) {
    if (genericType instanceof TypeVariable) {
      final Class<?> aClass = getRawType(classType);
      final Type type = resolveVariable((TypeVariable) genericType, aClass);

      if (type instanceof Class) {
        return (Class) type;
      }

      if (type instanceof ParameterizedType) {
        return (Class<?>) ((ParameterizedType) type).getRawType();
      }

      if (type instanceof TypeVariable && classType instanceof ParameterizedType) {
        final int index = ArrayUtilRt.find(aClass.getTypeParameters(), type);
        if (index >= 0) {
          return getRawType(getActualTypeArguments((ParameterizedType) classType)[index]);
        }
      }
    } else {
      return getRawType(genericType);
    }

    return null;
  }

  public static List<Field> collectFields(Class clazz) {
    List<Field> result = new ArrayList<Field>();
    collectFields(clazz, result);
    return result;
  }


  public static Field findField(Class clazz, Class type, String name) throws NoSuchFieldException {
    List<Field> fields = collectFields(clazz);
    for (Field each : fields) {
      if (name.equals(each.getName()) && (type == null || each.getType().equals(type))) return each;
    }

    throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
  }


  public static Field findAssignableField(Class clazz, Class type, String name) throws NoSuchFieldException {
    List<Field> fields = collectFields(clazz);
    for (Field each : fields) {
      if (name.equals(each.getName()) && type.isAssignableFrom(each.getType())) return each;
    }

    throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
  }

  private static void collectFields(Class clazz, List<Field> result) {
    final Field[] fields = clazz.getDeclaredFields();
    result.addAll(Arrays.asList(fields));
    final Class superClass = clazz.getSuperclass();

    if (superClass != null) {
      collectFields(superClass, result);
    }

    final Class[] interfaces = clazz.getInterfaces();
    for (Class each : interfaces) {
      collectFields(each, result);
    }
  }

  public static void resetField(Class clazz, Class type, String name) {
    try {
      resetField(null, findField(clazz, type, name));
    } catch (NoSuchFieldException ignore) {
    }
  }

  public static void resetField(Object object, Class type, String name) {
    try {
      resetField(object, findField(object.getClass(), type, name));
    } catch (NoSuchFieldException ignore) {
    }
  }

  public static void resetField(Object object, String name) {
    try {
      resetField(object, findField(object.getClass(), null, name));
    } catch (NoSuchFieldException ignore) {
    }
  }

  @SuppressWarnings("UnnecessaryBoxing")
  public static void resetField(final Object object, Field field) {
    field.setAccessible(true);
    Class<?> type = field.getType();
    try {
      if (type.isPrimitive()) {
        if (boolean.class.equals(type)) {
          field.set(object, Boolean.FALSE);
        } else if (int.class.equals(type)) {
          field.set(object, new Integer(0));
        } else if (double.class.equals(type)) {
          field.set(object, new Double(0));
        } else if (float.class.equals(type)) {
          field.set(object, new Float(0));
        }
      } else {
        field.set(object, null);
      }
    } catch (IllegalAccessException ignore) {
    }
  }


  public static Method findMethod(Method[] methods, String name, Class... parameters) {
    for (final Method method : methods) {
      if (name.equals(method.getName()) && Arrays.equals(parameters, method.getParameterTypes())) return method;
    }
    return null;
  }


  public static Method getMethod(Class aClass, String name, Class... parameters) {
    return findMethod(aClass.getMethods(), name, parameters);
  }


  public static Method getDeclaredMethod(Class aClass, String name, Class... parameters) {
    return findMethod(aClass.getDeclaredMethods(), name, parameters);
  }

  public static <T> T getField(Class objectClass, Object object, Class<T> type, String name) {
    try {
      final Field field = findAssignableField(objectClass, type, name);
      field.setAccessible(true);
      return (T) field.get(object);
    } catch (NoSuchFieldException e) {
      return null;
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  public static Type resolveVariableInHierarchy(TypeVariable variable, Class aClass) {
    Type type;
    Class current = aClass;
    while ((type = resolveVariable(variable, current, false)) == null) {
      current = current.getSuperclass();
      if (current == null) {
        return null;
      }
    }
    if (type instanceof TypeVariable) {
      return resolveVariableInHierarchy((TypeVariable) type, aClass);
    }
    return type;
  }


  public static <T> Constructor<T> getDefaultConstructor(Class<T> aClass) {
    try {
      final Constructor<T> constructor = aClass.getConstructor();
      constructor.setAccessible(true);
      return constructor;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("No default constructor in " + aClass, e);
    }
  }

  public static <T> T createInstance(Constructor<T> constructor, Object... args) {
    try {
      return constructor.newInstance(args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void resetThreadLocals() {
    try {
      Field field = Thread.class.getDeclaredField("threadLocals");
      field.setAccessible(true);
      field.set(Thread.currentThread(), null);
    } catch (Throwable ignore) {
    }
  }


  private static class MySecurityManager extends SecurityManager {
    private static final MySecurityManager INSTANCE = new MySecurityManager();

    public Class[] getStack() {
      return getClassContext();
    }
  }

  /**
   * Returns the class this method was called 'framesToSkip' frames up the caller hierarchy.
   * <p/>
   * NOTE:
   * <b>Extremely expensive!
   * Please consider not using it.
   * These aren't the droids you're looking for!</b>
   */
  public static Class findCallerClass(int framesToSkip) {
    try {
      Class[] stack = MySecurityManager.INSTANCE.getStack();
      int indexFromTop = 1 + framesToSkip;
      return stack.length > indexFromTop ? stack[indexFromTop] : null;
    } catch (Exception e) {
      return null;
    }
  }
}
