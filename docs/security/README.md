# Security Guide

[Back to the documentation portal](../README.md)

## Current Authentication Model

OptraBidz currently uses server-side HTTP sessions backed by application
accounts and session records. Spring Security owns request authentication,
authorization, CSRF protection, and security failure responses.

The security filter chain:

- creates sessions only when required;
- validates active, unexpired application sessions;
- uses cookie-based CSRF tokens with the `X-CSRF-TOKEN` request header;
- applies role rules at the HTTP boundary;
- adds request and security context to diagnostic logging; and
- returns the shared Problem Details contract for authentication and access
  failures.

Controllers may receive an authenticated principal and adapt it to an
application command. They do not verify credentials, parse authentication
tokens, or decide whether a session is valid.

## Authorization Boundary

Routes are classified as public, authenticated, or role-restricted in the
Spring Security configuration. Administrative routes require the administrator
role; startup and investor actions use their corresponding role boundaries.
Public webhook routes are authenticated by provider-specific signature and
replay checks rather than user sessions.

Endpoint accessibility is a business and security contract. Review both the
route rule and the owning service rule before changing it.

## Replaceable Authentication Adapters

Session authentication is the implemented adapter, not a business-domain
dependency. Application services consume authenticated identity and authority
information without owning the session mechanism. This boundary permits a
future JWT resource server or OAuth2 login adapter to be introduced without
rewriting domain rules.

JWT and OAuth2 are not implemented in the current repository. Adding either
requires a dedicated threat model, key or provider configuration, token
lifecycle rules, revocation strategy, migration plan, and security tests.

## Security Checks

- Keep secrets in environment-specific configuration, never in source or docs.
- Keep CSRF protection enabled for browser session flows.
- Return neutral not-found results when a more specific response would reveal
  another caller's resource.
- Test unauthenticated, wrong-role, expired-session, CSRF, webhook-signature,
  and replay paths at the HTTP boundary.
- Review CodeQL and dependency alerts without treating a green scan as a
  substitute for application-level security tests.

For public error behavior, read the [API error contract](../api/errors.md).
