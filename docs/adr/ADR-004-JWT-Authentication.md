# ADR-004 JWT Authentication

Status: Accepted

## Context

The application serves web and future mobile clients and requires stateless authentication.

## Decision

Use JWT access tokens and refresh tokens.

## Alternatives Considered

* Session-based authentication
* OAuth-only authentication

## Consequences

### Positive

* Stateless architecture
* Mobile friendly
* Scalable

### Negative

* Token revocation complexity
* Additional refresh token management

## Related Documents

* API_GUIDE.md
* ARCHITECTURE.md
