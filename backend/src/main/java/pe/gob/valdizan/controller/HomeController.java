package pe.gob.valdizan.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Única vista Thymeleaf: página de inicio del backend en "/". */
@Controller
public class HomeController {
    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${spring.application.name}") private String nombre;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("nombre", nombre);
        model.addAttribute("frontendUrl", frontendUrl);
        return "index";
    }
}
