package com.example.datingsite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WebLayerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void indexListsProfilesAndAppliesElvisDefault() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anna")))
                .andExpect(content().string(containsString("Maksym")))
                // у Maksym publicInfo == null -> текст за замовчуванням з Elvis-оператора
                .andExpect(content().string(containsString("Користувач ще нічого не розповів про себе.")));
    }

    @Test
    void searchFiltersByKeyword() throws Exception {
        mockMvc.perform(get("/").param("keyword", "футбол"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Oleg")))
                .andExpect(content().string(not(containsString("<h2>Iryna</h2>"))));
    }

    @Test
    void userInputIsEscapedToPreventXss() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String payload = "<script>alert('XSS')</script>";

        mockMvc.perform(post("/profiles/me/edit").session(session)
                        .param("name", "Anna")
                        .param("age", "22")
                        .param("publicInfo", payload)
                        .param("privateInfo", "anna@example.com")
                        .param("keywords", "музика"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles/1"));

        mockMvc.perform(get("/profiles/1").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString(payload))));
    }

    @Test
    void invalidFormShowsValidationErrors() throws Exception {
        mockMvc.perform(post("/profiles")
                        .param("name", "")
                        .param("age", "15"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Вкажіть ім’я")))
                .andExpect(content().string(containsString("Сайт доступний лише повнолітнім")));
    }

    @Test
    void invitationFlowChangesConnectionStatus() throws Exception {
        MockHttpSession anna = new MockHttpSession();
        MockHttpSession oleg = new MockHttpSession();
        mockMvc.perform(post("/switch-user").session(oleg).param("userId", "2"));

        // Anna (id=1) ще не бачить закритої інформації Oleg
        mockMvc.perform(get("/profiles/2").session(anna))
                .andExpect(content().string(containsString("Надіслати запрошення")))
                .andExpect(content().string(not(containsString("oleg@example.com"))));

        mockMvc.perform(post("/invitations").session(anna).param("receiverId", "2"))
                .andExpect(redirectedUrl("/profiles/2"));

        mockMvc.perform(get("/profiles/2").session(anna))
                .andExpect(content().string(containsString("Запрошення надіслано")));

        // Oleg отримав запрошення: бачить закриту інформацію Anna та кнопку «Прийняти»
        mockMvc.perform(get("/profiles/1").session(oleg))
                .andExpect(content().string(containsString("anna@example.com")))
                .andExpect(content().string(containsString("Прийняти")));

        mockMvc.perform(post("/invitations/1/accept").session(oleg))
                .andExpect(redirectedUrl("/invitations"));

        mockMvc.perform(get("/profiles/2").session(anna))
                .andExpect(content().string(containsString("Взаємна симпатія")))
                .andExpect(content().string(containsString("oleg@example.com")));
    }

    @Test
    void acceptingSomeoneElsesInvitationIsForbidden() throws Exception {
        MockHttpSession anna = new MockHttpSession();
        mockMvc.perform(post("/invitations").session(anna).param("receiverId", "2"));

        // Anna - відправник, а не отримувач, тому прийняти запрошення не може
        mockMvc.perform(post("/invitations/1/accept").session(anna))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Дію заборонено")));
    }

    @Test
    void unknownProfileReturns404Page() throws Exception {
        mockMvc.perform(get("/profiles/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Анкету з id=999 не знайдено")));
    }
}
