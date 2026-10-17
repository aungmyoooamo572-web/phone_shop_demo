package com.example.demo;

import com.example.demo.review.dao.ReviewRepository;
import com.example.demo.user.dao.UserRepository;
import com.example.demo.wishlist.dao.WishlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end HTTP checks for the write-authorization rules (phones, brands,
 * categories, admin order listing) and for the JSON error / image contracts.
 *
 * <p>Like {@link PhoneShopDemoApplicationTests} this starts the real application
 * against the MySQL database configured in application.properties. Every request
 * below is either read-only, rejected before any write happens, or runs as a
 * throw-away customer that is removed again before the test returns, so the
 * stored data is left exactly as it was found.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PhoneShopApiIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();

    /** A registered throw-away customer. */
    private record TempCustomer(String username, String token) {
    }

    /** Callback receiving a valid CUSTOMER access token. */
    @FunctionalInterface
    private interface TokenCheck {
        void run(String token) throws Exception;
    }

    private String baseUrl() {
        String port = environment.getProperty("local.server.port");
        assertThat(port).as("local.server.port (RANDOM_PORT)").isNotNull();
        return "http://localhost:" + port;
    }

    private HttpResponse<String> call(String method, String path, String token, String body)
            throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));

        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String loginToken(String username, String password) throws Exception {
        HttpResponse<String> response = call(
                "POST",
                "/api/users/login",
                null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"
        );

        assertThat(response.statusCode())
                .as("login response for %s: %s", username, response.body())
                .isEqualTo(200);

        String json = response.body();
        String marker = "\"token\":\"";
        int start = json.indexOf(marker);
        assertThat(start).as("token in login response: %s", json).isGreaterThan(-1);
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private String adminToken() throws Exception {
        return loginToken("admin", "admin123");
    }

    /**
     * Registers a throw-away CUSTOMER, runs {@code check} with a real customer
     * token and finally deletes the row again. Registration never creates a cart
     * or an order, so removing the user leaves no trace.
     */
    private void withCustomerToken(TokenCheck check) throws Exception {
        TempCustomer customer = registerCustomer();
        try {
            check.run(customer.token());
        } finally {
            deleteCustomer(customer.username());
        }
    }

    /** Registers a real CUSTOMER through the public endpoint. */
    private TempCustomer registerCustomer() throws Exception {
        String username = newUsername();

        HttpResponse<String> register = call(
                "POST",
                "/api/users/register",
                null,
                """
                {"username":"%s","email":"%s@example.com","password":"customer123",
                 "phone":"09123456789","address":"Integration Test"}
                """.formatted(username, username)
        );

        assertThat(register.statusCode())
                .as("register response: %s", register.body())
                .isEqualTo(201);

        return new TempCustomer(username, loginToken(username, "customer123"));
    }

    private String newUsername() {
        return "it_customer_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** Removes a throw-away customer together with anything it created. */
    private void deleteCustomer(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            reviewRepository.deleteAll(reviewRepository.findByUserId(user.getId()));
            wishlistRepository.findByUserId(user.getId()).forEach(wishlistRepository::delete);
            userRepository.delete(user);
        });
    }

    /** The id of an existing product, used to exercise the review/wishlist APIs. */
    private String firstPhoneId() throws Exception {
        HttpResponse<String> response = call("GET", "/api/phones", null, null);
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode phones = objectMapper.readTree(response.body());
        assertThat(phones.isArray()).as("GET /api/phones returns an array").isTrue();
        assertThat(phones.size()).as("seed data contains phones").isGreaterThan(0);
        return phones.get(0).path("id").asString();
    }

    // ---------------------------------------------------------------- catalog

    @Test
    @DisplayName("Public catalog browsing still works and the payload exposes isPrimary")
    void publicCatalogExposesIsPrimary() throws Exception {
        HttpResponse<String> response = call("GET", "/api/phones", null, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"isPrimary\"");
        assertThat(response.body()).doesNotContain("\"primary\"");
    }

    @Test
    @DisplayName("Brands and categories stay readable without a token")
    void brandsAndCategoriesStayPublic() throws Exception {
        assertThat(call("GET", "/api/brands", null, null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/api/categories", null, null).statusCode()).isEqualTo(200);
    }

    // ----------------------------------------------------------------- phones

    @Test
    @DisplayName("Anonymous users cannot create, update or delete phones")
    void anonymousCannotWritePhones() throws Exception {
        String id = UUID.randomUUID().toString();

        assertThat(call("POST", "/api/phones", null, "{}").statusCode()).isEqualTo(401);
        assertThat(call("PUT", "/api/phones/" + id, null, "{}").statusCode()).isEqualTo(401);
        assertThat(call("DELETE", "/api/phones/" + id, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("An ADMIN token passes the phone write rule (404 from the service, not 403)")
    void adminPassesPhoneWriteRule() throws Exception {
        HttpResponse<String> response =
                call("PUT", "/api/phones/" + UUID.randomUUID(), adminToken(), "{}");

        // The id does not exist, so authorization passed and the service answered 404.
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"message\"");
    }

    // ------------------------------------------------------- brands/categories

    @Test
    @DisplayName("Anonymous users cannot create, update or delete brands and categories")
    void anonymousCannotWriteBrandsOrCategories() throws Exception {
        String id = UUID.randomUUID().toString();

        for (String path : new String[]{"/api/brands", "/api/categories"}) {
            assertThat(call("POST", path, null, "{\"name\":\"x\"}").statusCode())
                    .as("anonymous POST %s", path)
                    .isEqualTo(401);
            assertThat(call("PUT", path + "/" + id, null, "{}").statusCode())
                    .as("anonymous PUT %s", path)
                    .isEqualTo(401);
            assertThat(call("DELETE", path + "/" + id, null, null).statusCode())
                    .as("anonymous DELETE %s", path)
                    .isEqualTo(401);
        }
    }

    @Test
    @DisplayName("A CUSTOMER token gets 403 on brand and category writes")
    void customerIsForbiddenFromBrandAndCategoryWrites() throws Exception {
        withCustomerToken(token -> {
            String id = UUID.randomUUID().toString();

            for (String path : new String[]{"/api/brands", "/api/categories"}) {
                HttpResponse<String> create =
                        call("POST", path, token, "{\"name\":\"x\"}");
                assertThat(create.statusCode()).as("customer POST %s", path).isEqualTo(403);
                assertThat(create.body()).contains("\"message\"");

                assertThat(call("PUT", path + "/" + id, token, "{}").statusCode())
                        .as("customer PUT %s", path)
                        .isEqualTo(403);
                assertThat(call("DELETE", path + "/" + id, token, null).statusCode())
                        .as("customer DELETE %s", path)
                        .isEqualTo(403);
            }
        });
    }

    @Test
    @DisplayName("An ADMIN token passes the brand/category write rule (405 = no handler, not 403)")
    void adminPassesBrandAndCategoryWriteRule() throws Exception {
        String token = adminToken();
        String id = UUID.randomUUID().toString();

        for (String path : new String[]{"/api/brands", "/api/categories"}) {
            // There is no update endpoint, so authorization passing shows up as 405
            // (method not supported) instead of 403. No write is performed.
            assertThat(call("PUT", path + "/" + id, token, "{}").statusCode())
                    .as("admin PUT %s", path)
                    .isEqualTo(405);
        }
    }

    // ----------------------------------------------------------------- orders

    @Test
    @DisplayName("Anonymous users cannot read orders and get a JSON message")
    void anonymousCannotReadOrders() throws Exception {
        HttpResponse<String> response = call("GET", "/api/orders", null, null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"message\"");
    }

    @Test
    @DisplayName("GET /api/orders/all is closed to anonymous users and to customers")
    void allOrdersIsAdminOnly() throws Exception {
        assertThat(call("GET", "/api/orders/all", null, null).statusCode()).isEqualTo(401);

        withCustomerToken(token -> {
            HttpResponse<String> denied = call("GET", "/api/orders/all", token, null);
            assertThat(denied.statusCode()).isEqualTo(403);
            assertThat(denied.body()).contains("\"message\"");
        });
    }

    @Test
    @DisplayName("ADMIN can list every order and no credential leaks into the payload")
    void adminCanListAllOrders() throws Exception {
        HttpResponse<String> response = call("GET", "/api/orders/all", adminToken(), null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("[");

        if (!response.body().equals("[]")) {
            assertThat(response.body()).contains("\"userId\"");
        }

        assertThat(response.body())
                .doesNotContain("\"password\"")
                .doesNotContain("\"token\"");
    }

    @Test
    @DisplayName("The literal /api/orders/all path is not swallowed by the /{orderId} mapping")
    void allOrdersPathBeatsTheOrderIdMapping() throws Exception {
        String token = adminToken();

        assertThat(call("GET", "/api/orders/all", token, null).statusCode()).isEqualTo(200);
        // A random UUID still resolves to the single-order lookup (404, not 400).
        assertThat(call("GET", "/api/orders/" + UUID.randomUUID(), token, null).statusCode())
                .isEqualTo(404);
    }

    // ---------------------------------------------------------------- errors

    @Test
    @DisplayName("Verifying a missing payment returns 404 (the locking lookup runs against MySQL)")
    void verifyMissingPaymentReturns404() throws Exception {
        HttpResponse<String> response = call(
                "POST",
                "/api/payments/" + UUID.randomUUID() + "/verify",
                adminToken(),
                null
        );

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"message\"");
    }

    @Test
    @DisplayName("A missing resource answers 404 with a message instead of 500")
    void missingResourceIsNotFound() throws Exception {
        HttpResponse<String> response =
                call("GET", "/api/phones/" + UUID.randomUUID(), null, null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"message\"");
    }

    @Test
    @DisplayName("An unsupported method keeps 405 instead of becoming a 500")
    void unsupportedMethodIsNotMaskedAs500() throws Exception {
        HttpResponse<String> response =
                call("PATCH", "/api/phones/" + UUID.randomUUID(), adminToken(), "{}");

        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    @DisplayName("Validation failures return 400 and duplicates return 409 with a message field")
    void validationAndDuplicateErrorsCarryAMessage() throws Exception {
        HttpResponse<String> invalid = call(
                "POST",
                "/api/users/register",
                null,
                "{\"username\":\"\",\"email\":\"not-an-email\",\"password\":\"\"}"
        );
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("\"message\"");

        HttpResponse<String> duplicate = call(
                "POST",
                "/api/users/register",
                null,
                "{\"username\":\"admin\",\"email\":\"admin@example.com\",\"password\":\"whatever123\"}"
        );
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("\"message\"");
    }

    // ------------------------------------------------- PhoneVariant JSON shape

    @Test
    @DisplayName("PhoneVariant JSON exposes isActive/isDeleted once, with no duplicate active/deleted keys")
    void variantJsonHasNoDuplicateFlags() throws Exception {
        HttpResponse<String> response = call("GET", "/api/phones", null, null);
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode phones = objectMapper.readTree(response.body());
        int variantsChecked = 0;

        for (JsonNode phone : phones) {
            for (JsonNode variant : phone.path("variants")) {
                variantsChecked++;

                assertThat(variant.has("isActive"))
                        .as("variant exposes isActive: %s", variant)
                        .isTrue();
                assertThat(variant.has("isDeleted"))
                        .as("variant exposes isDeleted: %s", variant)
                        .isTrue();

                // The old field names produced a second, differently named key.
                assertThat(variant.has("active"))
                        .as("no duplicate 'active' key: %s", variant)
                        .isFalse();
                assertThat(variant.has("deleted"))
                        .as("no duplicate 'deleted' key: %s", variant)
                        .isFalse();
            }
        }

        assertThat(variantsChecked)
                .as("the catalog payload carries phone variants")
                .isGreaterThan(0);
    }

    // --------------------------------------------------------------- reviews

    @Test
    @DisplayName("Reading a phone's reviews is public, writing them is not")
    void reviewReadIsPublicWriteIsNot() throws Exception {
        String phoneId = firstPhoneId();

        assertThat(call("GET", "/api/reviews/phone/" + phoneId, null, null).statusCode())
                .isEqualTo(200);

        // An unknown phone is a 404, not an empty 200.
        assertThat(call("GET", "/api/reviews/phone/" + UUID.randomUUID(), null, null).statusCode())
                .isEqualTo(404);

        // Own reviews and every write need a token.
        assertThat(call("GET", "/api/reviews/me", null, null).statusCode()).isEqualTo(401);
        assertThat(call("POST", "/api/reviews", null,
                "{\"phoneId\":\"" + phoneId + "\",\"rating\":4}").statusCode())
                .isEqualTo(401);

        withCustomerToken(token -> {
            // A customer cannot reach the admin-only order listing either, so the
            // new module did not widen any existing rule.
            assertThat(call("GET", "/api/reviews/me", token, null).statusCode()).isEqualTo(200);
        });
    }

    @Test
    @DisplayName("A review needs a valid rating and one review per customer and phone")
    void reviewValidationAndDuplicates() throws Exception {
        String phoneId = firstPhoneId();
        TempCustomer customer = registerCustomer();

        try {
            String ratingOutOfRange =
                    "{\"phoneId\":\"" + phoneId + "\",\"rating\":9,\"comment\":\"nope\"}";
            HttpResponse<String> invalid =
                    call("POST", "/api/reviews", customer.token(), ratingOutOfRange);
            assertThat(invalid.statusCode()).isEqualTo(400);
            assertThat(invalid.body()).contains("\"message\"");

            String valid = "{\"phoneId\":\"" + phoneId + "\",\"rating\":5,\"comment\":\"Excellent\"}";
            HttpResponse<String> created = call("POST", "/api/reviews", customer.token(), valid);
            assertThat(created.statusCode())
                    .as("create response: %s", created.body())
                    .isEqualTo(201);

            // Second review for the same phone is refused.
            HttpResponse<String> duplicate = call("POST", "/api/reviews", customer.token(), valid);
            assertThat(duplicate.statusCode()).isEqualTo(409);
            assertThat(duplicate.body()).contains("\"message\"");
        } finally {
            deleteCustomer(customer.username());
        }
    }

    @Test
    @DisplayName("Only the author may edit or delete a review (403 for everybody else)")
    void reviewOwnershipIsEnforcedOverHttp() throws Exception {
        String phoneId = firstPhoneId();
        TempCustomer author = registerCustomer();
        TempCustomer intruder = registerCustomer();

        try {
            HttpResponse<String> created = call(
                    "POST",
                    "/api/reviews",
                    author.token(),
                    "{\"phoneId\":\"" + phoneId + "\",\"rating\":5,\"comment\":\"Excellent\"}"
            );
            assertThat(created.statusCode()).isEqualTo(201);

            String reviewId = objectMapper.readTree(created.body()).path("id").asString();
            assertThat(reviewId).isNotBlank();

            // The public list carries the review but no private customer data.
            HttpResponse<String> publicList =
                    call("GET", "/api/reviews/phone/" + phoneId, null, null);
            assertThat(publicList.statusCode()).isEqualTo(200);
            assertThat(publicList.body()).contains(reviewId);
            assertThat(publicList.body())
                    .doesNotContain("@example.com")
                    .doesNotContain("\"password\"")
                    .doesNotContain("\"address\"");

            // Another customer can neither edit nor delete it.
            assertThat(call("PUT", "/api/reviews/" + reviewId, intruder.token(),
                    "{\"rating\":1,\"comment\":\"hacked\"}").statusCode())
                    .isEqualTo(403);
            assertThat(call("DELETE", "/api/reviews/" + reviewId, intruder.token(), null)
                    .statusCode())
                    .isEqualTo(403);

            // The author can.
            HttpResponse<String> updated = call("PUT", "/api/reviews/" + reviewId,
                    author.token(), "{\"rating\":2,\"comment\":\"Changed my mind\"}");
            assertThat(updated.statusCode()).isEqualTo(200);
            assertThat(updated.body()).contains("Changed my mind");

            assertThat(call("DELETE", "/api/reviews/" + reviewId, author.token(), null)
                    .statusCode())
                    .isEqualTo(204);
            assertThat(call("GET", "/api/reviews/phone/" + phoneId, null, null).body())
                    .doesNotContain(reviewId);
        } finally {
            deleteCustomer(author.username());
            deleteCustomer(intruder.username());
        }
    }

    // -------------------------------------------------------------- wishlist

    @Test
    @DisplayName("The wishlist is private: no duplicates, no cross-customer access")
    void wishlistIsPrivateAndDeduplicated() throws Exception {
        String phoneId = firstPhoneId();

        assertThat(call("GET", "/api/wishlist", null, null).statusCode()).isEqualTo(401);
        assertThat(call("POST", "/api/wishlist/" + phoneId, null, null).statusCode())
                .isEqualTo(401);

        TempCustomer owner = registerCustomer();
        TempCustomer other = registerCustomer();

        try {
            HttpResponse<String> added =
                    call("POST", "/api/wishlist/" + phoneId, owner.token(), null);
            assertThat(added.statusCode()).isEqualTo(201);
            assertThat(added.body()).contains(phoneId);

            // The same phone cannot be added twice.
            HttpResponse<String> duplicate =
                    call("POST", "/api/wishlist/" + phoneId, owner.token(), null);
            assertThat(duplicate.statusCode()).isEqualTo(409);

            // The other customer's list is untouched and they cannot remove the
            // owner's entry (it is simply not in their list).
            HttpResponse<String> otherList = call("GET", "/api/wishlist", other.token(), null);
            assertThat(otherList.statusCode()).isEqualTo(200);
            assertThat(otherList.body()).isEqualTo("[]");
            assertThat(call("DELETE", "/api/wishlist/" + phoneId, other.token(), null)
                    .statusCode())
                    .isEqualTo(404);
            assertThat(call("GET", "/api/wishlist", owner.token(), null).body())
                    .contains(phoneId);

            // The owner removes it.
            HttpResponse<String> removed =
                    call("DELETE", "/api/wishlist/" + phoneId, owner.token(), null);
            assertThat(removed.statusCode()).isEqualTo(200);
            assertThat(removed.body()).isEqualTo("[]");

            assertThat(call("DELETE", "/api/wishlist/" + phoneId, owner.token(), null)
                    .statusCode())
                    .isEqualTo(404);
        } finally {
            deleteCustomer(owner.username());
            deleteCustomer(other.username());
        }
    }
}
