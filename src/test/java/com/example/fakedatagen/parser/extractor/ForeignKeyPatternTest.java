package com.example.fakedatagen.parser.extractor;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForeignKeyPatternTest {
    
    @Test
    void testPattern() {
        String testLine = "ALTER CLASS [dba].[b] ADD CONSTRAINT [fk_b_a] FOREIGN KEY([a_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[a] ON DELETE RESTRICT ON UPDATE RESTRICT ;";
        
        Pattern fkPattern = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+FOREIGN\\s+KEY\\s*\\(([^)]+)\\).*?REFERENCES\\s+\\[(.*?)\\]\\.\\[(.*?)\\]",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        
        Matcher matcher = fkPattern.matcher(testLine);
        
        if (matcher.find()) {
            System.out.println("매칭 성공!");
            System.out.println("Group 1 (schema): " + matcher.group(1));
            System.out.println("Group 2 (table): " + matcher.group(2));
            System.out.println("Group 3 (constraint): " + matcher.group(3));
            System.out.println("Group 4 (columns): " + matcher.group(4));
            System.out.println("Group 5 (ref schema): " + matcher.group(5));
            System.out.println("Group 6 (ref table): " + matcher.group(6));
        } else {
            System.out.println("매칭 실패!");
        }
    }
}
