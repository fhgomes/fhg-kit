# Testing Framework — fhg-kit

> Testing standards, tools, and patterns. Every code change MUST have corresponding tests.

## 1. Unit Testing — JUnit 5

### Framework
- **JUnit 5 ONLY.** Never use JUnit 4 annotations (`@RunWith`, `org.junit.Test`) in new code.
- **Mockito** for mocking dependencies.
- **AssertJ** for fluent assertions (preferred over JUnit assertions).

### Test Class Structure
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("should create order with valid items")
    void shouldCreateOrderWithValidItems() {
        // given
        var request = CreateOrderDTO.builder()
            .customerName("John Doe")
            .items(List.of(orderItem()))
            .build();

        when(orderRepository.save(any(Order.class)))
            .thenReturn(savedOrder());

        // when
        var result = orderService.create(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isNotBlank();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }
}
```

### Naming Convention
- Test class: `<ClassUnderTest>Test` (e.g., `OrderServiceTest`).
- Test method: `should<ExpectedBehavior>` or `should<ExpectedBehavior>When<Condition>`.
- Use `@DisplayName` for human-readable descriptions.

**Examples:**
```java
@Test @DisplayName("should throw exception when order not found")
void shouldThrowExceptionWhenOrderNotFound() { ... }

@Test @DisplayName("should return empty list when no orders match filter")
void shouldReturnEmptyListWhenNoOrdersMatchFilter() { ... }

@Test @DisplayName("should mark order as paid and update status")
void shouldMarkOrderAsPaidAndUpdateStatus() { ... }
```

### Test Structure: Given / When / Then
Every test follows the AAA pattern (Arrange / Act / Assert), using comments for clarity:
```java
// given — set up test data and mocks
// when  — execute the method under test
// then  — verify the result and interactions
```

## 2. Integration Testing

### Spring Boot Integration Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("POST /api/orders should create order and return 201")
    void shouldCreateOrderAndReturn201() throws Exception {
        var request = """
            {
                "customerName": "John Doe",
                "items": [{"productId": "abc-123", "quantity": 2}]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderNumber").isNotEmpty())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

### Database: TestContainers (Real Database)
- **Use real databases for integration tests.** Never H2 or in-memory substitutes.
- TestContainers provides disposable Docker containers matching production.

```java
@Testcontainers
@SpringBootTest
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("should find orders by status using database query")
    void shouldFindOrdersByStatus() {
        // given
        orderRepository.save(buildOrder(OrderStatus.PENDING));
        orderRepository.save(buildOrder(OrderStatus.PAID));
        orderRepository.save(buildOrder(OrderStatus.PENDING));

        // when
        var result = orderRepository.findByStatus(OrderStatus.PENDING);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> o.getStatus() == OrderStatus.PENDING);
    }
}
```

## 3. E2E Testing — Playwright + TypeScript

### Framework
- **Playwright** with TypeScript.
- **Page Object Model (POM)** is mandatory.
- Tests run against a real running application.

### Directory Structure
```
e2e/
  pages/           # Page Objects — one per page/component
    login.page.ts
    orders.page.ts
    checkout.page.ts
  fixtures/        # Test data and custom fixtures
    test-data.ts
    auth.fixture.ts
  utils/           # Shared helpers
    api-helper.ts
    wait-utils.ts
  tests/           # Test specs
    login.spec.ts
    orders.spec.ts
    checkout.spec.ts
  playwright.config.ts
```

### Page Object Pattern
```typescript
// pages/orders.page.ts
import { Page, Locator } from '@playwright/test';

export class OrdersPage {
    readonly page: Page;
    readonly createOrderButton: Locator;
    readonly orderTable: Locator;
    readonly searchInput: Locator;

    constructor(page: Page) {
        this.page = page;
        this.createOrderButton = page.getByRole('button', { name: 'Create Order' });
        this.orderTable = page.getByRole('table', { name: 'Orders' });
        this.searchInput = page.getByLabel('Search orders');
    }

    async goto() {
        await this.page.goto('/orders');
    }

    async createOrder(data: OrderData) {
        await this.createOrderButton.click();
        // ... fill form
    }

    async searchOrders(query: string) {
        await this.searchInput.fill(query);
        await this.searchInput.press('Enter');
    }

    async getOrderCount(): Promise<number> {
        return await this.orderTable.getByRole('row').count() - 1; // minus header
    }
}
```

### Selector Strategy (Priority Order)
1. **Role** — `page.getByRole('button', { name: 'Submit' })` (preferred)
2. **Label** — `page.getByLabel('Email address')`
3. **Text** — `page.getByText('Welcome back')`
4. **Test ID** — `page.getByTestId('order-summary')` (last resort)

Never use CSS selectors or XPath unless absolutely necessary.

### Test Data
Centralize all test data in `fixtures/test-data.ts`:
```typescript
// fixtures/test-data.ts
export const testUsers = {
    admin: { email: 'admin@test.com', password: 'test-admin-123' },
    customer: { email: 'customer@test.com', password: 'test-customer-123' },
};

export const testOrders = {
    simpleOrder: {
        customerName: 'John Doe',
        items: [{ product: 'Widget A', quantity: 2 }],
    },
    bulkOrder: {
        customerName: 'Jane Corp',
        items: [
            { product: 'Widget A', quantity: 100 },
            { product: 'Widget B', quantity: 50 },
        ],
    },
};
```

## 4. Quality Gates

Every task (feature, bug fix, refactor) MUST pass ALL quality gates before completion:

| Gate | Criteria |
|------|----------|
| **Build** | `./gradlew build` succeeds with zero errors |
| **Unit Tests** | All unit tests pass |
| **Integration Tests** | All integration tests pass |
| **Files Exist** | All new files are created and properly structured |
| **Behavior Works** | The feature/fix works as specified |
| **No Regressions** | Existing tests still pass, no broken functionality |
| **Follows Patterns** | Code adheres to coding standards and project conventions |
| **Coverage** | New code has corresponding test coverage |

### Running Quality Gates
```bash
# Full quality gate check
./gradlew clean build          # compile + unit tests + integration tests
./gradlew test                 # unit tests only
./gradlew integrationTest      # integration tests only
npx playwright test            # E2E tests (when applicable)
```

## 5. Test Anti-Patterns (NEVER Do)

- **Never test implementation details.** Test behavior, not internal method calls.
- **Never use `Thread.sleep()` in tests.** Use `Awaitility` for async assertions.
- **Never share mutable state between tests.** Each test is independent.
- **Never mock the class under test.** Mock only its dependencies.
- **Never use `@MockBean` when `@Mock` + `@InjectMocks` suffice.** Reserve `@MockBean` for Spring context tests.
- **Never write tests that pass when the feature is broken.** If the test can't fail, it's not testing anything.
- **Never rely on test execution order.** Tests must be independent and idempotent.
- **Never use H2 or in-memory databases** to substitute for the real production database in integration tests. Use TestContainers.
