package com.hoangdev.Classroom.controller;


import com.hoangdev.Classroom.dto.NewsDto;
import com.hoangdev.Classroom.models.Classroom;
import com.hoangdev.Classroom.models.News;
import com.hoangdev.Classroom.service.classroom.ClassroomService;
import com.hoangdev.Classroom.service.news.NewsService;
import com.hoangdev.Classroom.service.user.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/news")
public class NewsController {
    @Autowired
    private NewsService newsService;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private UserService userService;


    @GetMapping("/index")
    public String getNewsInClass(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum, @RequestParam int classId, Model model){
        int pageSize = 4;
        List<NewsDto> newsDtoList = new ArrayList<>();
        Page<News> newsPage = newsService.findPaginated(classId, pageNum,pageSize);
        List<News> lstNew = newsPage.getContent();
        lstNew.forEach(news ->{
            var teacherList = userService.findByRoleAndNews("ROLE_TEACHER", news.getId());
            var studentList = userService.findByRoleAndNews("ROLE_STUDENT", news.getId());
            newsDtoList.add(new NewsDto(
                    news.getId(),
                    news.getTitle(),
                    news.getContent(),
                    news.getTimestamp(),
                    teacherList,
                    studentList));
        });
        model.addAttribute("classroomId", classId);
        model.addAttribute("newsDtoList", newsDtoList);
        model.addAttribute("currentPage", pageNum);
        model.addAttribute("totalsPage", newsPage.getTotalPages());
        model.addAttribute("totalItems", newsPage.getTotalElements());
        return "user/news";
    }

    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping("/add/{classroomId}")
    public String addNews(@PathVariable("classroomId") int classroomId, Model model) {
        model.addAttribute("classroomId", classroomId);
        model.addAttribute("news", new News());
        return "user/add-news";
    }

    @PostMapping()
    public String postAddNews(@RequestParam(value = "classId") int classId,@NotNull News news) {
        try{
            var currentUser = userService.getCurrentAccount();
            news.setTimestamp(new Date());
            news.setClassroom(classroomService.getClassById(classId));
            news.getUsers().add(currentUser);
            newsService.addNews(news);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return "redirect:/news/add/" + classId;
    }


    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping("/delete")
    public String deleteNews(@RequestParam(value = "classId") int classId,
                             @RequestParam(value = "newsId") Long newsId,
                             Model model) {

        News exitNews = newsService.getNewsByNewsId(newsId);
        Classroom exitClass = classroomService.findClass(classId);
        exitClass.deleteNews(exitNews);
        exitNews.getUsers().clear();
        exitClass.getNews().clear();
        newsService.deleteNews(exitNews);
        return "redirect:/news/index?classId=" + classId;
    }

}
