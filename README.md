# SupportManagement

## Leverantör

Sundsvalls kommun

## Beskrivning
SupportManagement tillhandahåller funktioner för att hantera ärenden.


## Tekniska detaljer

### Starta tjänsten

|Konfigurationsnyckel|Beskrivning|
|---|---|
|**Databasinställningar**||
|`spring.datasource.url`|JDBC-URL för anslutning till databas|
|`spring.datasource.username`|Användarnamn för anslutning till databas|
|`spring.datasource.password`|Lösenord för anslutning till databas|


### Paketera och starta tjänsten
Applikationen kan paketeras genom:

```
./mvnw package
```
Kommandot skapar filen `api-service-support-management-<version>.jar` i katalogen `target`. Tjänsten kan nu exekveras genom kommandot `java -jar target/api-service-support-management-<version>.jar`.

### Bygga och starta med Docker
Exekvera föjande kommando för att bygga en Docker-image:

```
docker build -f src/main/docker/Dockerfile -t api.sundsvall.se/ms-support-management:latest .
```

Exekvera följande kommando för att starta samma Docker-image i en container:

```
docker run -i --rm -p8080:8080 api.sundsvall.se/ms-support-management

```

#### Starta applikationen lokalt

Exekvera följande kommando för att bygga och starta en container i sandbox mode lokalt på port 1337:  

```
docker-compose -f src/main/docker/docker-compose-sandbox.yaml build && docker-compose -f src/main/docker/docker-compose-sandbox.yaml up
```


## 
Copyright (c) 2022 Sundsvalls kommun