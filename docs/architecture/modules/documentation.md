# Documentation Module

[Back to the module catalogue](README.md)

## Purpose

Assemble the public error reference, contribute RFC 9457 schemas to OpenAPI,
and fail closed when API documentation exposure is configured unsafely.

## Entry points

There is no module controller. Springdoc owns `/v3/api-docs` and Swagger UI;
`DocumentationSecurityConfiguration` controls their HTTP exposure.

## Application and domain

`PublicErrorCatalogue` normalizes module-owned descriptors into
`PublicErrorDefinition`. `ErrorCatalogueMarkdownRenderer` produces the checked
Markdown snapshot.

## Persistence

The module has no database entities or repositories. Its durable output is the
generated repository documentation checked by tests.

## Events

The module neither publishes nor consumes domain events.

## Dependencies

It intentionally imports public error catalogues from all business modules plus
shared framework and security problems, producing the widest read-only
documentation dependency surface.

## Security and errors

Exposure properties and validation prevent accidental public documentation in
restricted profiles. Documentation filtering does not alter runtime error
mapping.

## Verification

Twenty-two tests cover catalogue uniqueness and parity, OpenAPI components,
exposure rules, link/structure rules, diagrams, and real-HTTP documentation
smoke behavior.

## Known gaps

Documentation is repository- and Springdoc-backed; there is no independent
documentation service or automatic Confluence publication pipeline.
