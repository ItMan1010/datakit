package com.itman.datakit.admin.common.util;

import com.google.common.base.Joiner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/09/03  12:33
 */

public class CommonUtil {
    private CommonUtil() {
    }

    public static String getLocalTime(String format) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    public static String genMapKey(List<String> mapKeyList) {
        return MAP_KEY_PREFIX + Joiner.on(",").join(mapKeyList);
    }

    public static String makeTableFieldNullFlag(Integer nullAble) {
        if (Objects.isNull(nullAble)) {
            return "default null";
        }

        switch (nullAble) {
            case 0:
                return "not null";
            case 1:
                return "default null";
            default:
                break;
        }
        return "default null";
    }

    public static String formatDate(String date) {
        StringBuilder sb = new StringBuilder(date);
        sb.insert(4, '-');
        sb.insert(7, '-');
        return sb.toString();
    }
}
