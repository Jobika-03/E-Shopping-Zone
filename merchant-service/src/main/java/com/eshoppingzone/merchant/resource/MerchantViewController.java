package com.eshoppingzone.merchant.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MerchantViewController {

    @GetMapping({"/merchant/ui", "/portal", "/dashboard"})
    public String forwardToUi() {
        return "forward:/index.html";
    }
}
