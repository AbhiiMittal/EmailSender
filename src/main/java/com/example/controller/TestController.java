package com.example.controller;

import com.example.helper.ExcelEmailHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final ExcelEmailHandler excelEmailHandler;

    @GetMapping("/run-excel")
    public String runExcel() {
        String file = "Book1.xlsx";
        String user = "test_user";
        String executionTime = "2026-03-03T18:39:00";
        String subject = "Please complete your registration";
        String body = "<p style=\\\"text-align: center;\\\"><strong>Welcome to KnowledgeGeek</strong></p>\\n\" +\n" +
                "            \"<p style=\\\"text-align: left;\\\">Humanity&apos;s biggest weapon, curse or boon whatever we believe was, is and will always be Knowledge.</p>\\n\" +\n" +
                "            \"<p style=\\\"text-align: left;\\\">We at Knowledge Geek truly believe that every human deserves to enhance their knowledge and become a better self for themselves, our mission is to provide this knowledge to every corner of the world to every being with the most affordable cost (we will try our best to get the course without using any penny).</p>\\n\" +\n" +
                "            \"<p style=\\\"text-align: left;\\\">If you also believe in our mission then please visit us here &nbsp;- <a href=\\\"https://knowledgegeek.in\\\">KNOWLEDGE GEEK</a></p>\\n\" +\n" +
                "            \"<img src=\\\"https://spreadgreatideas.org/wp-content/uploads/2024/03/SGI.org-Quote-Template-627.png\\\" height=400 width = 650>\\n\" +\n" +
                "            \"<p style=\\\"text-align: left;\\\"><br></p>\\n\" +\n" +
                "            \"<p style='text-align: left; color: rgb(0, 0, 0); font-size: 16px; font-family: -apple-system, BlinkMacSystemFont, \\\"\\\";'>Thanks</p>\\n\" +\n" +
                "            \"<p style='text-align: left; color: rgb(0, 0, 0); font-size: 16px; font-family: -apple-system, BlinkMacSystemFont, \\\"\\\";'>Team KnowledgeGeek</p>\\n\" +\n" +
                "            \"<p style='text-align: left; color: rgb(0, 0, 0); font-size: 16px; font-family: -apple-system, BlinkMacSystemFont, \\\"\\\";'><br></p>\\n\" +\n" +
                "            \"<p style='text-align: center; color: rgb(0, 0, 0); font-size: 16px; font-family: -apple-system, BlinkMacSystemFont, \\\"\\\";'><a href=\\\"https://www.youtube.com\\\">Unsubscribe</a></p>";
        excelEmailHandler.processAndSaveExcel(file, user,executionTime,subject,body);
        return "Excel processing triggered";
    }
}
