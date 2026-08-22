package com.medisync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalyticsWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testSummary() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary"))
               .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testTimeseries() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/timeseries?days=30"))
               .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testActivity() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/user-activity?days=30"))
               .andDo(print());
    }
}
