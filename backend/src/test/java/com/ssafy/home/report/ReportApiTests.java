package com.ssafy.home.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.report.type.ReportReason;
import com.ssafy.home.report.type.ReportStatus;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_report;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ReportApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanCreatePropertyReport() throws Exception {
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        Property property = saveProperty(null, PropertyStatus.APPROVED);
        String buyerToken = tokenFor(buyer);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "FAKE_LISTING",
                                  "content": "The listing information looks suspicious."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(property.getPropertyId()))
                .andExpect(jsonPath("$.data.userId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.reason").value("FAKE_LISTING"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Report report = reportRepository.findAll().get(0);
        assertThat(report.getReason()).isEqualTo(ReportReason.FAKE_LISTING);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getCreatedAt()).isNotNull();
        assertThat(report.getUpdatedAt()).isNotNull();
        assertThat(report.getProcessedAt()).isNull();
    }

    @Test
    void agentCanReportOtherOwnersApprovedProperty() throws Exception {
        User owner = saveUser("owner@example.com", Role.AGENT);
        User reporter = saveUser("reporter@example.com", Role.AGENT);
        Property property = saveProperty(owner.getId(), PropertyStatus.APPROVED);
        String reporterToken = tokenFor(reporter);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("PRICE_MISMATCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(reporter.getId()))
                .andExpect(jsonPath("$.data.propertyId").value(property.getPropertyId()));
    }

    @Test
    void ownerAndAdminCannotCreatePropertyReport() throws Exception {
        User owner = saveUser("owner@example.com", Role.AGENT);
        User admin = saveUser("admin@example.com", Role.ADMIN);
        Property property = saveProperty(owner.getId(), PropertyStatus.APPROVED);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + tokenFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("PHOTO_MISMATCH")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("PHOTO_MISMATCH")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void duplicatePropertyReportFromSameUserIsRejected() throws Exception {
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        Property property = saveProperty(null, PropertyStatus.APPROVED);
        String buyerToken = tokenFor(buyer);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("PRICE_MISMATCH")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("PHOTO_MISMATCH")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REPORT_ALREADY_EXISTS"));
    }

    @Test
    void anonymousUserCannotCreatePropertyReport() throws Exception {
        Property property = saveProperty(null, PropertyStatus.APPROVED);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("NO_CONTACT")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedPropertyCannotBeReported() throws Exception {
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        Property property = saveProperty(null, PropertyStatus.DELETED);
        String buyerToken = tokenFor(buyer);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", property.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("FRAUD_SUSPECTED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));
    }

    @Test
    void nonApprovedPropertyCannotBeReported() throws Exception {
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        String buyerToken = tokenFor(buyer);
        Property pending = saveProperty(null, PropertyStatus.PENDING);
        Property rejected = saveProperty(null, PropertyStatus.REJECTED);
        Property hidden = saveProperty(null, PropertyStatus.HIDDEN);

        mockMvc.perform(post("/api/properties/{propertyId}/reports", pending.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("FRAUD_SUSPECTED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));

        mockMvc.perform(post("/api/properties/{propertyId}/reports", rejected.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("FRAUD_SUSPECTED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));

        mockMvc.perform(post("/api/properties/{propertyId}/reports", hidden.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("FRAUD_SUSPECTED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));
    }

    @Test
    void adminCanReadReportListDetailAndUpdateReviewStatus() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN);
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        Property property = saveProperty(null, PropertyStatus.APPROVED);
        Report report = saveReport(buyer, property, ReportReason.FRAUD_SUSPECTED);
        String adminToken = tokenFor(admin);

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reportId").value(report.getId()))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mockMvc.perform(get("/api/admin/reports")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/admin/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(report.getId()))
                .andExpect(jsonPath("$.data.reason").value("FRAUD_SUSPECTED"));

        mockMvc.perform(patch("/api/admin/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "HIDDEN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.processedAt").exists());

        Report updatedReport = reportRepository.findById(report.getId()).orElseThrow();
        Property updatedProperty = propertyRepository.findById(property.getPropertyId()).orElseThrow();
        assertThat(updatedReport.getStatus()).isEqualTo(ReportStatus.HIDDEN);
        assertThat(updatedReport.getProcessedAt()).isNotNull();
        assertThat(updatedProperty.getStatus()).isEqualTo(PropertyStatus.APPROVED);
    }

    @Test
    void nonAdminUsersCannotAccessAdminReportApis() throws Exception {
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        Property property = saveProperty(null, PropertyStatus.APPROVED);
        Report report = saveReport(buyer, property, ReportReason.ETC);
        String buyerToken = tokenFor(buyer);

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotReadMissingReport() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN);
        String adminToken = tokenFor(admin);

        mockMvc.perform(get("/api/admin/reports/{reportId}", 9999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"));
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .name(role.name())
                .nickname(email.substring(0, email.indexOf("@")))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Property saveProperty(Long ownerId, PropertyStatus status) {
        return propertyRepository.save(Property.builder()
                .ownerId(ownerId)
                .title("Test property")
                .address("Seoul Gangnam 1")
                .sido("Seoul")
                .gugun("Gangnam")
                .dong("Yeoksam")
                .dataSource(ownerId == null ? DataSource.PUBLIC : DataSource.AGENT)
                .status(status)
                .build());
    }

    private Report saveReport(User user, Property property, ReportReason reason) {
        return reportRepository.save(Report.builder()
                .user(user)
                .property(property)
                .reason(reason)
                .content("Test report content")
                .build());
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    private String reportJson(String reason) {
        return """
                {
                  "reason": "%s",
                  "content": "Test report content"
                }
                """.formatted(reason);
    }
}
