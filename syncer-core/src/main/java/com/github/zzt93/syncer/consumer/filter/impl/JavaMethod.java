package com.github.zzt93.syncer.consumer.filter.impl;

import com.github.zzt93.syncer.ShutDownCenter;
import com.github.zzt93.syncer.config.common.InvalidConfigException;
import com.github.zzt93.syncer.config.syncer.SyncerFilterMeta;
import com.github.zzt93.syncer.data.util.SyncFilter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaMethod {

  private static final Logger logger = LoggerFactory.getLogger(JavaMethod.class);

  public static SyncFilter build(String consumerId, SyncerFilterMeta filterMeta, String method) {
    String source = getClassSource(method);

    String className = "Filter" + consumerId;
    source = source.replaceFirst("MethodFilterTemplate", className);
    Path path = Paths.get(filterMeta.getSrc(), className + ".java");
    try {
      Files.write(path, source.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      logger.error("No permission", e);
    }

    compile(path.toString());

    Class<?> cls;
    try {
      URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{path.getParent().toUri().toURL()}, JavaMethod.class.getClassLoader());
      cls = Class.forName(className, true, classLoader);
      return (SyncFilter) cls.newInstance();
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | MalformedURLException e) {
      ShutDownCenter.initShutDown(e);
      return null;
    }
  }

  public static String getClassSource(String method) {
    return "import com.github.zzt93.syncer.data.*;\n" +
        "import com.github.zzt93.syncer.data.es.*;\n" +
        "import com.github.zzt93.syncer.data.util.*;\n" +
        "import java.util.*;\n" +
        "import java.util.stream.*;\n" +
        "import java.math.BigDecimal;\n" +
        "import java.sql.Timestamp;\n" +
        "import java.sql.Date;\n" +
        "import org.slf4j.Logger;\n" +
        "import org.slf4j.LoggerFactory;\n" +
        "\n" +
        "public class MethodFilterTemplate implements SyncFilter<SyncData> {\n" +
        "\n" +
        "  private final Logger logger = LoggerFactory.getLogger(getClass());\n" +
        "\n" +
        addNewline(method) +
        "\n" +
        "}\n";
  }

  private static void compile(String path) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostic -> logger.error("{}, {}", diagnostic.getLineNumber(), diagnostic.getSource().toUri()), null, null);
    try {
      fm.setLocation(StandardLocation.CLASS_PATH, Lists.newArrayList(new File(System.getProperty("java.class.path"))));
    } catch (IOException e) {
      logger.error("Fail to set location for compiler file manager", e);
    }
    if (compiler.run(null, null, null, path) != 0) {
      ShutDownCenter.initShutDown(new InvalidConfigException());
    }
  }

  private static String addNewline(String method) {
    char[] cs = method.toCharArray();
    StringBuilder sb = new StringBuilder(method.length() + 50);
    boolean inQuote = false;
    for (int i = 0; i < cs.length; i++) {
      char c = cs[i];
      sb.append(c);
      switch (c) {
        case '"':
        case '\'':
          inQuote = !inQuote;
          break;
        case ';':
        case '{':
        case '}':
          if (!inQuote) sb.append('\n');
          break;
      }
    }
    return sb.toString();
  }
}
