package pt.psoft.g1.psoftg1.bibliographicmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BibliographicComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer sources;

    @BeforeEach
    void setUp() {
        sources = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void composesControllerServiceRegistryAndSources() throws Exception {
        sources.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://www.googleapis.com/books/v1/volumes")))
                .andRespond(withSuccess("""
                        {"items":[{"volumeInfo":{
                          "title":"O Príncipe",
                          "authors":["Nicolau Maquiavel"],
                          "description":"Descrição",
                          "publisher":"Editora",
                          "publishedDate":"1532",
                          "pageCount":128,
                          "categories":["Política"],
                          "language":"pt"
                        }}]}
                        """, MediaType.APPLICATION_JSON));

        sources.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://openlibrary.org/api/books")))
                .andRespond(withSuccess("""
                        {"ISBN:9789720706386":{
                          "title":"O Príncipe",
                          "authors":[{"name":"Nicolau Maquiavel"}],
                          "publishers":[{"name":"Editora"}],
                          "publish_date":"1532",
                          "number_of_pages":128,
                          "subjects":[{"name":"Política"}],
                          "cover":{"medium":"https://example.com/cover.jpg"}
                        }}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/books/9789720706386/bibliographic")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("O Príncipe"))
                .andExpect(jsonPath("$.authors[0]").value("Nicolau Maquiavel"))
                .andExpect(jsonPath("$.sources[0]").value("google-books"))
                .andExpect(jsonPath("$.sources[1]").value("open-library"));

        sources.verify();
    }
}
