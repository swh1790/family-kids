package com.family.kids.controller;

import com.family.kids.service.ChildService;
import com.family.kids.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pets")
public class PetController {

    @Autowired
    private PetService petService;
    @Autowired
    private ChildService childService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("children", childService.listActive());
        model.addAttribute("levels", petService.listLevels());
        return "pets";
    }

    @GetMapping("/{childId}")
    public String petCard(@PathVariable Long childId, Model model) {
        model.addAttribute("card", petService.getPetCard(childId));
        model.addAttribute("levels", petService.listLevels());
        return "pet-card";
    }
}
