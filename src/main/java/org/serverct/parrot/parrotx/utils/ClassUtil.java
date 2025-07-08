package org.serverct.parrot.parrotx.utils;

import org.serverct.parrot.parrotx.PPlugin;
import org.serverct.parrot.parrotx.utils.i18n.I18n;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * From CSDN
 * https://blog.csdn.net/jdzms23/article/details/17550119
 *
 * @since v1.4.7.5-alpha
 */
public class ClassUtil {
    /*
     * 取得某一类所在包的所有类名 不含迭代
     */
    public static String[] getPackageAllClassName(String classLocation, String packageName) {
        //将packageName分解
        String[] packagePathSplit = packageName.split("[.]");
        StringBuilder realClassLocation = new StringBuilder(classLocation);
        for (String s : packagePathSplit) {
            realClassLocation.append(File.separator).append(s);
        }
        File packageDir = new File(realClassLocation.toString());
        if (packageDir.isDirectory()) {
            return packageDir.list();
        }
        return null;
    }

    /**
     * 从包package中获取所有的Class（修正版）
     */
    public static List<Class<?>> getClasses(PPlugin plugin) {
        List<Class<?>> classes = new ArrayList<>();
        boolean recursive = true;
        String packageName = plugin.getClass().getPackage().getName();
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = plugin.getClass().getClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName) && name.endsWith(".class") && !entry.isDirectory()) {
                                int idx = name.lastIndexOf('/');
                                String currentPackageName = (idx != -1) ? name.substring(0, idx).replace('/', '.') : packageName;
                                if (currentPackageName.contains("util") || currentPackageName.contains("parrotx")) {
                                    continue;
                                }
                                String className = name.substring(0, name.length() - 6).replace('/', '.');
                                try {
                                    classes.add(Class.forName(className));
                                } catch (Throwable e) {
                                    System.out.println("[ClassUtil] 加载类失败: " + className + " - " + e.getMessage());
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     */
    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath,
                                                        final boolean recursive, List<Class<?>> classes) {
        //获取此包的目录 建立一个File
        File dir = new File(packagePath);
        //如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        //如果存在 就获取包下的所有文件 包括目录
        //自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
        File[] dirFiles = dir.listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class"
        )));
        //循环所有文件
        for (File file : dirFiles) {
            //如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(),
                        file.getAbsolutePath(),
                        recursive,
                        classes);
            } else {
                //如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    //添加到集合中去
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
