package com.itman.datakit.admin.common.util;

import org.springframework.ui.Model;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/10/13  10:53
 */

public class ResultModelUtil {
    public static void resultModel(Model model, String resultCode, String resultMessage) {
        model.addAttribute("resultCode", resultCode);
        model.addAttribute("resultMessage", resultMessage);
        model.addAttribute("resultHref", "null");
    }

    public static void resultModel(Model model, String resultCode, String resultMessage, String resultHref) {
        model.addAttribute("resultCode", resultCode);
        model.addAttribute("resultMessage", resultMessage);
        model.addAttribute("resultHref", resultHref);
    }
}
