# Error Disclosure Flow

[Back to architecture](../README.md)

## Runtime mapping

1. Validation, security, application, or unexpected failures reach their
   boundary handler.
2. `RestExceptionHandler`, `ProblemAuthenticationEntryPoint`, or
   `ProblemAccessDeniedHandler` selects the public error descriptor.
3. `ProblemDetailsFactory` creates the RFC 9457 response with a stable code,
   safe detail, request ID, timestamp, and optional validation violations.
4. Internal exception text and diagnostic context remain in server-side logs;
   stack traces, SQL, secrets, and provider credentials are not returned.

## Documentation mapping

Each business module owns its public error catalogue. The `documentation`
module combines those catalogues with framework and security problems for
OpenAPI and the generated Markdown catalogue. Runtime exception handling does
not query the documentation catalogue.

## Neutral disclosure

When a precise response would confirm another actor's resource or provider
ownership, the boundary uses a neutral not-found result. The payment webhook
provider-mismatch path is one current example.

See the [API error contract](../../api/errors.md) and
[public catalogue](../../api/error-catalogue.md) for the client-facing model.
