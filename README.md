# SupportManagement

_Provides features for managing cases related to support related functions. It includes functionalities such as creating, updating, and tracking errand statuses and progress._

## Getting Started

### Prerequisites

- **Java 21 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone git@github.com:Sundsvallskommun/api-service-support-management.git
   cd api-service-support-management
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#Configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible. See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## Dependencies

This microservice depends on the following services:

- **EmailReader**
  - **Purpose:** Reads e-mails sent to mailboxes and provides them for processing by SupportManagement and other systems.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-email-reader)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Employee**
  - **Purpose:** Used for reading employee information.
  - **Repository:** Not available at this moment.
  - **Additional Notes:** Employee is a API serving data from [Metadatakatalogen](https://utveckling.sundsvall.se/digital-infrastruktur/metakatalogen).
- **Eventlog**
  - **Purpose:** Used for logging events
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-eventlog)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Messaging**
  - **Purpose:** Used to send communications to stakeholders via E-mail, SMS or Open-E Webmessage
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Notes**
  - **Purpose:** Provides functionality for storing and retrieving notes linked to an organization or a citizen.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-notes)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **WebMessageCollector**
  - **Purpose:** Reads web messages sent to open-E and provides them for processing by SupportManagement and other systems.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-web-message-collector)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
  - 

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

Alternatively, refer to the `openapi.yml` file located in `src/test/resources/api` for the OpenAPI specification.

## Usage

### API Endpoints

Refer to the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X GET http://localhost:8080/api/2281/my.namespace/errands/b82bd8ac-1507-4d9a-958d-369261eecc15/communication
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in `application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **Database Settings:**

  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/your_database
      username: your_db_username
      password: your_db_password
  ```
- **External Service URLs:**

  ```yaml
  integration:
    emailreader:
      url: http://dependency_service_url
    employee:
      url: http://dependency_service_url
    eventlog:
      url: http://dependency_service_url
    messaging:
      url: http://dependency_service_url
    notes:
      url: http://dependency_service_url
    web-message-collector:
      url: http://dependency_service_url

  spring:
    security:
      oauth2:
        client:
          provider:
            emailreader:
              token-uri: http://dependecy_service_token_url
            employee:
              token-uri: http://dependecy_service_token_url
            eventlog:
              token-uri: http://dependecy_service_token_url
            messaging:
              token-uri: http://dependecy_service_token_url
            notes:
              token-uri: http://dependecy_service_token_url
            web-message-collector:
              token-uri: http://dependecy_service_token_url
          registration:
            emailreader:
              client-id: the-client-id
              client-secret: the-client-secret
            employee:
              client-id: the-client-id
              client-secret: the-client-secret
            eventlog:
              client-id: the-client-id
              client-secret: the-client-secret
  ```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)

---

Copyright (c) 2023 Sundsvalls kommun
