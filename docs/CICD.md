# CICD.md

# CI/CD Pipeline

Project: E-Commerce Platform

Automation: GitHub Actions

Registry: GitHub Container Registry (`ghcr.io/asadujjaman47/ecommerce`)

---

## 1. Overview

The pipeline implements the stages defined in `DEPLOYMENT.md` §22 — Build → Unit Tests →
Integration Tests → Static Analysis → Docker Build → Docker Push → Deploy → Smoke Tests — across
four workflow files. It follows the GitHub Flow described in `GIT_WORKFLOW.md`: short-lived
feature branches, PR-driven validation, squash-merge into a protected `main`, and tag-based
releases.

```
PR / branch push ─▶ CI (build-test + CodeQL + docker build & scan)
merge to main ─────▶ Release (build-test ─▶ push image to GHCR + scan)
v* tag ────────────▶ Release (build-test ─▶ push :<semver> image + scan)
manual dispatch ───▶ Deploy (gated SSH compose pull/up + health smoke test)
```

---

## 2. Workflows

### `build.yml` — reusable build + test gate

- Trigger: `workflow_call` only (no standalone run).
- Runs `./mvnw -B -ntp clean verify` on Temurin 21 with Maven caching: unit tests, Testcontainers
  integration tests, Spring Modulith verification, and the `build-info` goal.
- Testcontainers starts its own Postgres/Redis, so **no database secrets are needed**. The
  surefire config disables rate limiting; `JWT_SECRET` falls back to a dev value under the default
  profile.
- Uploads Surefire/Failsafe reports and the built jar as artifacts.

### `ci.yml` — pull request validation

- Trigger: `pull_request` → `main`. (PR-only by design: pushes to `main` are gated by
  `release.yml`, which reuses the same `build.yml`, so CI does not also run on `push` — that would
  double every check on a PR'd branch.)
- Jobs:
  - `build-test` — reuses `build.yml`.
  - `codeql` — CodeQL Java SAST (manual build mode: `mvnw -DskipTests package`).
  - `docker-build` — builds the image with Buildx (`push: false`) to validate the Dockerfile, then
    scans it for `CRITICAL,HIGH` OS/library CVEs by running the official **`aquasec/trivy:0.65.0`**
    container (deterministic — avoids the `trivy-action` binary installer, which is rate-limited on
    hosted runners). Trivy does **not** fail the PR (`exit-code: 0`); the SARIF lands in the Security
    tab.
- `concurrency` cancels superseded runs on the same ref.

### `release.yml` — build & publish image

- Triggers: `push` to `main`; `push` tags matching `v*`.
- Jobs:
  - `build-test` — reuses `build.yml` (re-gates tag pushes that never saw a PR).
  - `publish` — logs in to GHCR with `GITHUB_TOKEN`, computes the lowercase image name once (a
    `Compute image name` step, since the owner is mixed-case and GHCR requires lowercase), derives
    tags via `docker/metadata-action`, builds & pushes, then scans the pushed digest with the
    `aquasec/trivy:0.65.0` container (pulling from GHCR with the job credentials).
- Image tags: `latest` + `sha-<short>` on `main`; `<semver>` + `<major>.<minor>` on `v*`.

### `deploy.yml` — gated deployment

- Trigger: `workflow_dispatch` with inputs `environment` (staging|production) and `image_tag`.
- Binds to a GitHub **Environment** (`environment: ${{ inputs.environment }}`) so you can require
  manual approval. SSHes to the host (which holds `docker-compose.yml` + `.env`), runs
  `docker compose pull app && up -d app` with `APP_IMAGE` set to the chosen GHCR tag, then polls
  `/actuator/health` for `"status":"UP"` (30 × 5s) as the smoke test.

---

## 3. Secrets & variables

| Where                 | Name              | Purpose                                            |
| --------------------- | ----------------- | -------------------------------------------------- |
| Auto                  | `GITHUB_TOKEN`    | GHCR push (`packages: write`) in `release.yml`.    |
| Environment secret    | `DEPLOY_SSH_HOST` | Deploy target host.                                |
| Environment secret    | `DEPLOY_SSH_USER` | SSH user.                                           |
| Environment secret    | `DEPLOY_SSH_KEY`  | SSH private key (PEM).                              |
| Environment secret    | `DEPLOY_SSH_PORT` | Optional SSH port (default 22).                     |
| Environment secret    | `GHCR_TOKEN`      | PAT (`read:packages`) for host-side `docker login`.|
| Repo/env variable     | `APP_PORT`        | Optional; health-check port (default 8080).         |

---

## 4. One-time manual setup (not scriptable from the repo)

1. **Branch protection** on `main`: require pull requests and the `build-test` + `CodeQL` checks;
   disable force-push and direct commits.
2. **Environments**: create `staging` and `production`; add required reviewers so `deploy.yml`
   pauses for approval.
3. **Secrets**: add the deploy secrets above (per environment).
4. **Deploy host**: provision Docker + Docker Compose; place `docker-compose.yml` and a populated
   `.env` (default dir `/opt/ecommerce`, overridable via `DEPLOY_DIR`). The compose `app` service
   reads its image from `APP_IMAGE`, so the host pulls the published GHCR image instead of building.

---

## 5. Running a deployment

1. Merge to `main` (or push a `v*` tag) → `Release` publishes the image to GHCR.
2. Actions → **Deploy** → *Run workflow* → pick the environment and image tag.
3. Approve the environment gate.
4. The job pulls, restarts the app, and fails if `/actuator/health` is not `UP` within 150s.

---

## 6. Pinned versions

Actions are pinned to Node24-native majors so the workflows raise no Node20 / CodeQL-v3 deprecation
warnings. When bumping, confirm the target tag exists first.

| Action | Pin |
| --- | --- |
| `actions/checkout` | `v7` |
| `actions/setup-java` | `v5` |
| `actions/upload-artifact` | `v7` |
| `docker/setup-buildx-action` | `v4` |
| `docker/login-action` | `v4` |
| `docker/metadata-action` | `v6` |
| `docker/build-push-action` | `v7` |
| `github/codeql-action/*` | `v4` |
| `appleboy/ssh-action` | `v1` |
| Trivy (run as a container) | `aquasec/trivy:0.65.0` |

Build toolchain: Temurin **21**, Spring Boot 3.5.x. Bumping the JDK means updating `actions/setup-java`
(`java-version`), the `Dockerfile` base images, and `pom.xml` (`java.version`) together.
