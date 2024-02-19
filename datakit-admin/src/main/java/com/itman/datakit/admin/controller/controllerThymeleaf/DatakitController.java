package com.itman.datakit.admin.controller.controllerThymeleaf;

import com.itman.datakit.admin.common.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.util.ResultModelUtil.resultModel;

@Slf4j
@Controller
@RequestMapping("/datakit")
public class DatakitController {
    /**
     * 系统首页
     * http://127.0.0.1:9193/datakit/index
     */
    @GetMapping(value = "/index")
    public String index(Model model) {
        UserDTO userDTO = new UserDTO();
        model.addAttribute("userDTO", userDTO);
        return "indexPage";
    }

    /**
     * 登录请求
     * http://127.0.0.1:9193/datakit/login
     */
    @PostMapping(value = "/login")
    public String login(Model model, UserDTO userDTO) {
        if (Objects.isNull(userDTO.getUser()) || StringUtils.isEmpty(userDTO.getUser().getUsername()) || StringUtils.isEmpty(userDTO.getUser().getPassword()) ||
                !userDTO.getUser().getUsername().equals("admin") || !userDTO.getUser().getPassword().equals("admin")) {
            resultModel(model, ERROR, "验证失败，请正确输入登录信息!");
            return "indexPage";
        } else {
            resultModel(model, SUCCESS, "登录成功!");
            return "redirect:./taskInstanceRows/query?pageNum=1";
        }
    }
}
