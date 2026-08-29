# Request and Security Flow

[Back to architecture](../README.md)

## Session-authenticated request

1. `SecurityConfig` classifies the route and applies public, authenticated, or
   role-restricted policy.
2. `ActiveSessionFilter` validates the server-side application session and
   supplies the authenticated principal.
3. `CsrfCookieFilter` participates in the browser CSRF contract for state
   changes.
4. The controller translates HTTP input and authenticated identity into an
   application command or query.
5. The owning service enforces resource ownership and business rules; route
   authorization is not a substitute for service authorization.
6. A repository adapter commits the accepted state transition.

Authentication belongs to the security adapter. Controllers may consume the
already authenticated principal, but they do not validate passwords, sessions,
JWTs, or OAuth2 tokens.

## Rejection path

`ProblemAuthenticationEntryPoint` handles missing or invalid authentication;
`ProblemAccessDeniedHandler` handles authenticated callers without sufficient
authority. Both use the shared Problem Details writer so security failures
match the public API error shape without disclosing internal diagnostics.

## Provider webhook request

Provider callbacks intentionally bypass user-session authentication. The
financial ingress instead applies body-size, provider configuration, HMAC
signature, timestamp tolerance, strict parsing, provider ownership, and replay
controls before business processing. A mismatched provider receives the same
neutral not-found outcome as an unknown payment attempt.

JWT resource-server and OAuth2 login adapters are future work. The current
domain and application rules should continue to consume an authenticated actor
contract so those adapters can be added without moving authentication logic
into controllers.
