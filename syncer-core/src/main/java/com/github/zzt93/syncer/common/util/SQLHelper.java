package com.github.zzt93.syncer.common.util;

import com.github.zzt93.syncer.data.util.SQLFunction;
import com.github.zzt93.syncer.producer.input.mysql.AlterMeta;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * @author zzt
 */
public class SQLHelper {

  private static final Logger logger = LoggerFactory.getLogger(SQLHelper.class);

  public static String inSQL(Object value) {
    if (value == null) {
      return "NULL";
    }
    Class<?> aClass = value.getClass();
    if (ClassUtils.isPrimitiveOrWrapper(aClass)
        || CharSequence.class.isAssignableFrom(aClass)
        || value instanceof Timestamp
        || value instanceof BigDecimal) {
      if (value instanceof String) {
        // TODO 2019/3/3 http://www.jguru.com/faq/view.jsp?EID=8881 {escape '/'} ?
        String replace = StringUtils.replace(StringUtils.replace(value.toString(), "'", "''"), "\\", "\\\\");
        value = "'" + replace + "'";
      } else if (value instanceof Timestamp) {
        value = "'" + value.toString() + "'";
      }
    } else if (SQLFunction.class.isAssignableFrom(aClass)) {
      value = value.toString();
    } else if (aClass == byte[].class) {
      value = "0x" + Hex.encodeHexString(((byte[]) value));
    } else {
      logger.error("Unhandled complex type: {}, value: {}", aClass, value);
    }
    return value.toString();
  }

  public static String wrapCol(String col) {
    return '`' + col + '`';
  }

  private static String isAlter(String sql) {
    String[] words = {"alter ", "table "};
    String lower = sql.toLowerCase();

    int alterIndex = lower.indexOf(words[0]);
    int tableIndex = lower.indexOf(words[1], alterIndex + words[0].length());
    if (alterIndex != -1 && tableIndex != -1) {
      int afterTable = tableIndex + words[1].length();
      int alter = isAlter(lower, afterTable);
      if (alter != -1) {
        return null;
      }
      int addIndex = isAddAfter(lower, afterTable);
      if (addIndex != -1) {
        return sql.substring(afterTable, addIndex);
      }
      int dropIndex = isDrop(lower, afterTable);
      if (dropIndex != -1) {
        return sql.substring(afterTable, dropIndex);
      }
      int modifyIndex = isModify(lower, afterTable);
      if (modifyIndex != -1) {
        return sql.substring(afterTable, modifyIndex);
      }
    }
    return null;
  }

  private static int isAddAfter(String sql, int afterTable) {
    // alter table xx add yy after zz
    int i = sql.indexOf(" add ", afterTable);
    if (sql.indexOf(" after ", i) != -1) {
      return i;
    }
    return -1;
  }

  private static int isDrop(String sql, int afterTable) {
    // alter table xx drop column yy
    return sql.indexOf(" drop ", afterTable);
  }

  private static int isModify(String sql, int afterTable) {
    // alter table xx modify column yy after zz
    int i = sql.indexOf(" modify ", afterTable);
    if (sql.indexOf(" after ", i) != -1) {
      return i;
    }
    return -1;
  }

  private static int isAlter(String sql, int afterTable) {
    // alter table xx alter column credit_total drop default
    return sql.indexOf(" alter ", afterTable);
  }

  /**
   * https://dev.mysql.com/doc/refman/5.7/en/alter-table.html
   */
  public static AlterMeta alterMeta(String database, String sql) {
    String alterTarget = isAlter(sql);
    if (alterTarget != null) {
      alterTarget = alterTarget.replaceAll("`|\\s", "");
      if (!StringUtils.isEmpty(database)) {
        return new AlterMeta(database, alterTarget);
      }
      String[] split = alterTarget.split("\\.");
      assert split.length == 2;
      return new AlterMeta(split[0], split[1]);
    }
    return null;
  }

}
