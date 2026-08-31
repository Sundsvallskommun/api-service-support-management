# OpenSearch PoC on RHEL — Runbook

Topology: **OpenSearch + the app on the RHEL lab box**, MariaDB is the **shared test-cluster database**
(real test data — no dump transfer needed). Flyway is disabled in the `opensearch-poc` profile: the test
database schema is owned by the deployed service and this PoC instance must never migrate it.

Built from branch `opensearch-poc` — same code as the Elasticsearch PoC, but the client layer is
`spring-data-opensearch` (the official elasticsearch-java client refuses to talk to OpenSearch servers).

## 1. Install Java 25 (Temurin)

```bash
sudo tee /etc/yum.repos.d/adoptium.repo > /dev/null <<'EOF'
[Adoptium]
name=Adoptium
baseurl=https://packages.adoptium.net/artifactory/rpm/rhel/$releasever/$basearch
enabled=1
gpgcheck=1
gpgkey=https://packages.adoptium.net/artifactory/api/gpg/key/public
EOF
sudo dnf install -y temurin-25-jre
java -version
```

## 2. Run OpenSearch (Docker, security disabled)

The box has Docker, so OpenSearch runs as a container — exactly the image, flags and memory profile
validated in the local rehearsal. Bound to 127.0.0.1 so only the app on the same box reaches it:

```bash
docker run -d --name opensearch --restart unless-stopped \
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

Teardown when done: `docker rm -f opensearch` (add `docker volume prune` if you mounted a data volume).

<details>
<summary>Alternative: native RPM install (no Docker)</summary>

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

</details>

## 3. Transfer the application

On the Mac (branch `opensearch-poc`):

```bash
mvn package -DskipTests
scp target/api-service-support-management-*.jar <user>@<rhel-host>:~/support-management-opensearch-poc.jar
```

## 4. Run the application

Fill in the test-cluster MariaDB coordinates. First run with reindex on:

```bash
SPRING_PROFILES_ACTIVE=opensearch-poc \
SPRING_DATASOURCE_URL='jdbc:mariadb://<testcluster-host>:3306/<database>' \
SPRING_DATASOURCE_USERNAME='<user>' \
SPRING_DATASOURCE_PASSWORD='<password>' \
ELASTICSEARCH_REINDEX_ONSTARTUP=true \
java -jar ~/support-management-opensearch-poc.jar
```

(Keep it in the foreground first time to watch the logs; `nohup ... &` or a tmux session once happy.
Subsequent starts: drop `ELASTICSEARCH_REINDEX_ONSTARTUP=true` — the index is maintained incrementally.)

Watch for these log lines:

- `Found 1 Elasticsearch repository interface` — repository activation OK
- `Starting Elasticsearch reindex of errands with json parameters`
- `Elasticsearch reindex finished: N of M errands indexed, 0 failed batches`

## 5. Verify

```bash
# Index document count (errands that have jsonParameters)
curl -s http://localhost:9200/errand/_count?pretty

# Cross-schema search directly against OpenSearch — pick a value you know exists in test data
curl -s -X POST 'http://localhost:9200/errand/_search' -H 'Content-Type: application/json' -d '
{"query":{"bool":{"must":[{"query_string":{"query":"\"<some-value>\"","default_field":"jsonParameters.*","lenient":true}}]}},"size":3}'

# Hybrid query through the app (namespace/municipality from real test data)
curl -s 'http://localhost:8080/2281/<NAMESPACE>/errands?jsonParameterFilter=%22<some-value>%22&page=0&size=5'

# Graceful degradation: stop OpenSearch, the endpoint must still answer (filter ignored, warning logged)
sudo systemctl stop opensearch
curl -s -o /dev/null -w "%{http_code}\n" 'http://localhost:8080/2281/<NAMESPACE>/errands?jsonParameterFilter=%22x%22&page=0&size=5'
sudo systemctl start opensearch
```

If you want to query the app from outside the box, open the port:

```bash
sudo firewall-cmd --add-port=8080/tcp --permanent && sudo firewall-cmd --reload
```

OpenSearch itself stays bound to 127.0.0.1 — only the app talks to it.

## Notes

- The shared test DB is used **read-mostly**: the reindex reads errands, searches read. Only calls to this
  instance's own REST API would write. Do not point Draken/other clients at this instance.
- The index name is `errand`; delete and re-run reindex with
  `curl -X DELETE http://localhost:9200/errand` + restart with the reindex flag.
- Local end-to-end rehearsal of exactly this build exists in `run-poc.sh` (compose now runs OpenSearch).

