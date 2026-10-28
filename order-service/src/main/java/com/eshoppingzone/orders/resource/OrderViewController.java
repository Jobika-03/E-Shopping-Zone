package com.eshoppingzone.orders.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderViewController {

    @GetMapping({"/ui", "/portal", "/dashboard"})
    public String forwardToUi() {
        return "forward:/index.html";
    }
}
