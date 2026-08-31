# OpenSearch PoC on RHEL — Runbook

Topology: **OpenSearch + the app as Docker containers on the RHEL lab box**, MariaDB is the **shared
test-cluster database** (real test data — no dump transfer needed). Flyway is disabled in the
`opensearch-poc` profile: the test database schema is owned by the deployed service and this PoC
instance must never migrate it.

Built from branch `opensearch-poc` — same code as the Elasticsearch PoC, but the client layer is
`spring-data-opensearch` (the official elasticsearch-java client refuses to talk to OpenSearch servers).

Everything runs in Docker, so the box needs nothing installed beyond Docker itself — the app image
bundles its own Java 25 runtime.

## 1. Run OpenSearch

Same image, flags and memory profile as the validated local rehearsal. The containers share a Docker
network; 9200 is also published on 127.0.0.1 for the verification curls:

```bash
docker network create poc
docker run -d --name opensearch --restart unless-stopped --network poc \
  -p 127.0.0.1:9200:9200 \
  -e discovery.type=single-node \
  -e DISABLE_SECURITY_PLUGIN=true \
  -e DISABLE_PERFORMANCE_ANALYZER_AGENT_CLI=true \
  -e OPENSEARCH_JAVA_OPTS="-Xms512m -Xmx512m" \
  docker.io/opensearchproject/opensearch:3.2.0
```

Sizing notes from the rehearsal: 512 MB heap is plenty for this dataset; the performance analyzer agent
(a second JVM inside the container) is disabled because it costs several hundred MB. If the box has
memory to spare, add `--memory 2g` as a guard rail; with a 1 GB heap budget ~2.5 GB for the container.

Verify:

```bash
curl -s http://localhost:9200 | head -20
curl -s http://localhost:9200/_cluster/health?pretty
```

Expect `"distribution" : "opensearch"` and cluster status green/yellow.

## 2. Build and transfer the application image

On the Mac (branch `opensearch-poc`) — build the jar and ship jar + Dockerfile; the image itself is
built on the box so no large image transfer is needed:

```bash
mvn package -DskipTests
scp target/api-service-support-management-*.jar <user>@<rhel-host>:~/poc/
scp docker/Dockerfile <user>@<rhel-host>:~/poc/
```

On the RHEL box:

```bash
cd ~/poc && mkdir -p target && mv api-service-support-management-*.jar target/ \
  && docker build -t support-management-opensearch-poc -f Dockerfile .
```

(The Dockerfile copies `target/*.jar`, hence the target/ directory.)

## 3. Run the application

Fill in the test-cluster MariaDB coordinates. First run with reindex on:

```bash
docker run -d --name support-management --network poc \
  -p 8080:8080 --memory 1536m \
  -e SPRING_PROFILES_ACTIVE=opensearch-poc \
  -e SPRING_DATASOURCE_URL='jdbc:mariadb://<testcluster-host>:3306/<database>' \
  -e SPRING_DATASOURCE_USERNAME='<user>' \
  -e SPRING_DATASOURCE_PASSWORD='<password>' \
  -e OPENSEARCH_URIS=http://opensearch:9200 \
  -e ELASTICSEARCH_REINDEX_ONSTARTUP=true \
  support-management-opensearch-poc
docker logs -f support-management
```

`OPENSEARCH_URIS` points at the opensearch container over the shared network. For subsequent starts
drop `ELASTICSEARCH_REINDEX_ONSTARTUP=true` — the index is maintained incrementally.

Watch for these log lines:

- `Found 1 Elasticsearch repository interface` — repository activation OK
- `Starting Elasticsearch reindex of errands with json parameters`
- `Elasticsearch reindex finished: N of M errands indexed, 0 failed batches`

## 4. Verify

```bash
# Index document count (errands that have jsonParameters)
curl -s http://localhost:9200/errand/_count?pretty

# Cross-schema search directly against OpenSearch — pick a value you know exists in test data
curl -s -X POST 'http://localhost:9200/errand/_search' -H 'Content-Type: application/json' -d '
{"query":{"bool":{"must":[{"query_string":{"query":"\"<some-value>\"","default_field":"jsonParameters.*","lenient":true}}]}},"size":3}'

# Hybrid query through the app (namespace/municipality from real test data)
curl -s 'http://localhost:8080/2281/<NAMESPACE>/errands?jsonParameterFilter=%22<some-value>%22&page=0&size=5'

# Graceful degradation: stop OpenSearch, the endpoint must still answer (filter ignored, warning logged)
docker stop opensearch
curl -s -o /dev/null -w "%{http_code}\n" 'http://localhost:8080/2281/<NAMESPACE>/errands?jsonParameterFilter=%22x%22&page=0&size=5'
docker start opensearch
```

If you want to query the app from outside the box, open the port:

```bash
sudo firewall-cmd --add-port=8080/tcp --permanent && sudo firewall-cmd --reload
```

OpenSearch itself stays bound to 127.0.0.1 (and the poc network) — only the app talks to it.

## Teardown

```bash
docker rm -f support-management opensearch && docker network rm poc
```

## Notes

- The shared test DB is used **read-mostly**: the reindex reads errands, searches read. Only calls to this
  instance's own REST API would write. Do not point Draken/other clients at this instance.
- The index name is `errand`; delete and re-run reindex with
  `curl -X DELETE http://localhost:9200/errand` + restart the app container with the reindex flag.
- Local end-to-end rehearsal of exactly this build exists in `run-poc.sh` (compose now runs OpenSearch).

<details>
<summary>Alternative: OpenSearch as native RPM install (no container)</summary>

The RPM bundles its own JDK, so this still needs no Java install:

```bash
sudo curl -SL https://artifacts.opensearch.org/releases/bundle/opensearch/3.x/opensearch-3.x.repo \
  -o /etc/yum.repos.d/opensearch-3.x.repo
# The RPM install requires an initial admin password even though security is disabled afterwards
sudo env OPENSEARCH_INITIAL_ADMIN_PASSWORD='Throwaway-Passw0rd!' dnf install -y opensearch
sudo tee -a /etc/opensearch/opensearch.yml > /dev/null <<'EOF'
discovery.type: single-node
network.host: 127.0.0.1
plugins.security.disabled: true
EOF
sudo sed -i 's/^-Xms.*/-Xms512m/; s/^-Xmx.*/-Xmx512m/' /etc/opensearch/jvm.options
sudo systemctl enable --now opensearch
# Leave the opensearch-performance-analyzer service stopped
```

The app container then needs `--add-host` or host networking to reach it, since it is not on the poc
network — simplest is `-e OPENSEARCH_URIS=http://host.docker.internal:9200` with
`--add-host=host.docker.internal:host-gateway`.

</details>

