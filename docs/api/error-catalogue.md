# Public Error Catalogue

This file is generated from the application-owned public error definitions.
Do not add diagnostic or secret values.

| Code | Category | HTTP | Title | Safe detail | Type | Sources |
|---|---|---:|---|---|---|---|
| `ACCOUNT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested account was not found | `urn:optrabidz:problem:account-not-found` | `identity` |
| `ACCOUNT_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The account state does not allow this operation | `urn:optrabidz:problem:account-state-conflict` | `identity` |
| `ACTIVE_ADMIN_ALREADY_EXISTS` | `CONFLICT` | 409 | Request conflict | An active administrator already exists | `urn:optrabidz:problem:active-admin-already-exists` | `participation-admin` |
| `ACTIVE_ADMIN_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | No active administrator was found | `urn:optrabidz:problem:active-admin-not-found` | `participation-admin` |
| `ADMIN_AUTHORITY_ALREADY_GRANTED` | `CONFLICT` | 409 | Request conflict | Administrator authority was previously granted to this account | `urn:optrabidz:problem:admin-authority-already-granted` | `participation-admin` |
| `ADMIN_AUTHORITY_UNAVAILABLE` | `CONFLICT` | 409 | Request conflict | No active administrator authority is available for transfer | `urn:optrabidz:problem:admin-authority-unavailable` | `governance` |
| `ADMIN_RECOVERY_ACCESS_DENIED` | `AUTHORIZATION` | 403 | Access denied | Admin recovery access was denied | `urn:optrabidz:problem:admin-recovery-access-denied` | `governance` |
| `AGREEMENT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested agreement was not found | `urn:optrabidz:problem:agreement-not-found` | `marketplace` |
| `AUTHENTICATION_REQUIRED` | `AUTHENTICATION` | 401 | Authentication required | Authentication is required | `urn:optrabidz:problem:authentication-required` | `spring-security` |
| `AUTHORIZATION_FAILED` | `AUTHORIZATION` | 403 | Access denied | You are not authorized to perform this action | `urn:optrabidz:problem:authorization-failed` | `participation, security-application, spring-security` |
| `BID_ACCEPTANCE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The bid cannot be accepted in the current marketplace state | `urn:optrabidz:problem:bid-acceptance-conflict` | `marketplace` |
| `BID_ALREADY_EXISTS` | `CONFLICT` | 409 | Request conflict | An active bid already exists for this listing | `urn:optrabidz:problem:bid-already-exists` | `marketplace` |
| `BID_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested bid was not found | `urn:optrabidz:problem:bid-not-found` | `marketplace` |
| `BID_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The requested action conflicts with the current bid state | `urn:optrabidz:problem:bid-state-conflict` | `marketplace` |
| `CREDENTIAL_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested credential was not found | `urn:optrabidz:problem:credential-not-found` | `security-application` |
| `CSRF_VALIDATION_FAILED` | `AUTHORIZATION` | 403 | Request security validation failed | Request security validation failed | `urn:optrabidz:problem:csrf-validation-failed` | `spring-security` |
| `CURRENT_PASSWORD_INVALID` | `AUTHENTICATION` | 401 | Authentication required | Current password is incorrect | `urn:optrabidz:problem:current-password-invalid` | `security-application` |
| `EMAIL_ALREADY_REGISTERED` | `CONFLICT` | 409 | Request conflict | Email is already registered | `urn:optrabidz:problem:email-already-registered` | `security-application` |
| `ENDPOINT_NOT_FOUND` | `TRANSPORT` | 404 | Endpoint not found | The requested endpoint is unavailable | `urn:optrabidz:problem:endpoint-not-found` | `spring-mvc` |
| `FINANCIAL_OPERATION_NOT_ALLOWED` | `AUTHORIZATION` | 403 | Access denied | This financial operation is not allowed | `urn:optrabidz:problem:financial-operation-not-allowed` | `financial` |
| `GOVERNANCE_ACTION_NOT_ELIGIBLE` | `BUSINESS_RULE` | 422 | Business rule violation | The requested action does not satisfy governance eligibility rules | `urn:optrabidz:problem:governance-action-not-eligible` | `governance` |
| `GOVERNANCE_ACTION_NOT_PERMITTED` | `AUTHORIZATION` | 403 | Access denied | The requested action is not permitted by governance policy | `urn:optrabidz:problem:governance-action-not-permitted` | `governance` |
| `GOVERNANCE_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The requested action conflicts with the current governed state | `urn:optrabidz:problem:governance-state-conflict` | `governance` |
| `INTERNAL_SERVER_ERROR` | `TRANSPORT` | 500 | Internal server error | An unexpected error occurred | `urn:optrabidz:problem:internal-server-error` | `spring-mvc` |
| `INVALID_CREDENTIALS` | `AUTHENTICATION` | 401 | Authentication required | Invalid email or password | `urn:optrabidz:problem:invalid-credentials` | `security-application` |
| `INVESTOR_ALREADY_EXISTS` | `CONFLICT` | 409 | Request conflict | An investor profile already exists | `urn:optrabidz:problem:investor-already-exists` | `participation-investor` |
| `INVESTOR_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested investor profile was not found | `urn:optrabidz:problem:investor-not-found` | `participation-investor` |
| `INVESTOR_PREFERENCE_ALREADY_EXISTS` | `CONFLICT` | 409 | Request conflict | The investor preference already exists | `urn:optrabidz:problem:investor-preference-already-exists` | `classification` |
| `INVESTOR_PREFERENCE_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested investor preference was not found | `urn:optrabidz:problem:investor-preference-not-found` | `classification` |
| `INVESTOR_PREFERENCE_PROFILE_REQUIRED` | `BUSINESS_RULE` | 422 | Business rule violation | Create an investor profile before managing preferences | `urn:optrabidz:problem:investor-preference-profile-required` | `classification` |
| `INVESTOR_PREFERENCE_RULE_VIOLATION` | `BUSINESS_RULE` | 422 | Business rule violation | The investor preference does not satisfy preference rules | `urn:optrabidz:problem:investor-preference-rule-violation` | `classification` |
| `LISTING_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested listing was not found | `urn:optrabidz:problem:listing-not-found` | `marketplace` |
| `LISTING_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The requested action conflicts with the current listing state | `urn:optrabidz:problem:listing-state-conflict` | `marketplace` |
| `MALFORMED_REQUEST` | `TRANSPORT` | 400 | Malformed request | The request body is malformed | `urn:optrabidz:problem:malformed-request` | `spring-mvc` |
| `MARKETPLACE_ACCESS_DENIED` | `AUTHORIZATION` | 403 | Access denied | You are not authorized to perform this marketplace action | `urn:optrabidz:problem:marketplace-access-denied` | `marketplace` |
| `METHOD_NOT_ALLOWED` | `TRANSPORT` | 405 | Method not allowed | The HTTP method is not supported for this endpoint | `urn:optrabidz:problem:method-not-allowed` | `spring-mvc` |
| `NOTIFICATION_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested notification was not found | `urn:optrabidz:problem:notification-not-found` | `notification` |
| `NOTIFICATION_SUBSCRIPTION_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested notification subscription was not found | `urn:optrabidz:problem:notification-subscription-not-found` | `notification` |
| `NOT_ACCEPTABLE` | `TRANSPORT` | 406 | Response type not acceptable | The requested response media type is not available | `urn:optrabidz:problem:not-acceptable` | `spring-mvc` |
| `PASSWORD_POLICY_VIOLATION` | `VALIDATION` | 400 | Request validation failed | Password must contain at least one letter and one digit | `urn:optrabidz:problem:password-policy-violation` | `security-application` |
| `PAYMENT_ALREADY_CONFIRMED` | `CONFLICT` | 409 | Request conflict | The payment has already been confirmed | `urn:optrabidz:problem:payment-already-confirmed` | `financial` |
| `PAYMENT_ATTEMPT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested payment attempt was not found | `urn:optrabidz:problem:payment-attempt-not-found` | `financial` |
| `PAYMENT_INTENT_EXPIRED` | `CONFLICT` | 409 | Request conflict | The payment intent has expired | `urn:optrabidz:problem:payment-intent-expired` | `financial` |
| `PAYMENT_INTENT_NOT_ACTIVE` | `CONFLICT` | 409 | Request conflict | The payment intent is not active | `urn:optrabidz:problem:payment-intent-not-active` | `financial` |
| `PAYMENT_INTENT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested payment intent was not found | `urn:optrabidz:problem:payment-intent-not-found` | `financial` |
| `PAYMENT_METHOD_UNSUPPORTED` | `BUSINESS_RULE` | 422 | Business rule violation | The selected payment method is not supported | `urn:optrabidz:problem:payment-method-unsupported` | `financial` |
| `PAYMENT_PROVIDER_MISMATCH` | `BUSINESS_RULE` | 422 | Business rule violation | The payment attempt cannot be handled by this provider | `urn:optrabidz:problem:payment-provider-mismatch` | `financial` |
| `PAYMENT_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The payment state no longer permits this operation | `urn:optrabidz:problem:payment-state-conflict` | `financial` |
| `PAYMENT_WEBHOOK_PAYLOAD_INVALID` | `VALIDATION` | 400 | Request validation failed | The webhook payload is invalid | `urn:optrabidz:problem:payment-webhook-payload-invalid` | `financial` |
| `PAYMENT_WEBHOOK_PROCESSING_FAILED` | `INTERNAL` | 500 | Internal server error | The webhook could not be processed | `urn:optrabidz:problem:payment-webhook-processing-failed` | `financial` |
| `PAYMENT_WEBHOOK_REJECTED` | `VALIDATION` | 400 | Request validation failed | The webhook request was rejected | `urn:optrabidz:problem:payment-webhook-rejected` | `financial` |
| `PROFILE_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The profile state does not allow this operation | `urn:optrabidz:problem:profile-state-conflict` | `identity` |
| `REPAYMENT_INSTALLMENT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested repayment installment was not found | `urn:optrabidz:problem:repayment-installment-not-found` | `financial` |
| `REPAYMENT_INSTALLMENT_NOT_PAYABLE` | `CONFLICT` | 409 | Request conflict | The repayment installment cannot be paid in its current state | `urn:optrabidz:problem:repayment-installment-not-payable` | `financial` |
| `REPAYMENT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested repayment was not found | `urn:optrabidz:problem:repayment-not-found` | `financial` |
| `REPAYMENT_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The repayment state no longer permits this operation | `urn:optrabidz:problem:repayment-state-conflict` | `financial` |
| `SELF_REGISTRATION_NOT_ALLOWED` | `BUSINESS_RULE` | 422 | Business rule violation | Only startup or investor accounts can self-register | `urn:optrabidz:problem:self-registration-not-allowed` | `security-application` |
| `SETTLEMENT_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested settlement was not found | `urn:optrabidz:problem:settlement-not-found` | `financial` |
| `SETTLEMENT_NOT_PAYABLE` | `CONFLICT` | 409 | Request conflict | The settlement cannot be paid in its current state | `urn:optrabidz:problem:settlement-not-payable` | `financial` |
| `SETTLEMENT_STATE_CONFLICT` | `CONFLICT` | 409 | Request conflict | The settlement state no longer permits this operation | `urn:optrabidz:problem:settlement-state-conflict` | `financial` |
| `STARTUP_ALREADY_EXISTS` | `CONFLICT` | 409 | Request conflict | A startup profile already exists | `urn:optrabidz:problem:startup-already-exists` | `participation-startup` |
| `STARTUP_CLASSIFICATION_ALREADY_EXISTS` | `CONFLICT` | 409 | Request conflict | The startup classification already exists | `urn:optrabidz:problem:startup-classification-already-exists` | `classification` |
| `STARTUP_CLASSIFICATION_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested startup classification was not found | `urn:optrabidz:problem:startup-classification-not-found` | `classification` |
| `STARTUP_CLASSIFICATION_PROFILE_REQUIRED` | `BUSINESS_RULE` | 422 | Business rule violation | Create a startup profile before managing classifications | `urn:optrabidz:problem:startup-classification-profile-required` | `classification` |
| `STARTUP_CLASSIFICATION_RULE_VIOLATION` | `BUSINESS_RULE` | 422 | Business rule violation | The startup classification does not satisfy classification rules | `urn:optrabidz:problem:startup-classification-rule-violation` | `classification` |
| `STARTUP_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested startup profile was not found | `urn:optrabidz:problem:startup-not-found` | `participation-startup` |
| `UNSUPPORTED_FUNDING_MODEL` | `BUSINESS_RULE` | 422 | Business rule violation | The requested funding model is not supported | `urn:optrabidz:problem:unsupported-funding-model` | `marketplace` |
| `UNSUPPORTED_MEDIA_TYPE` | `TRANSPORT` | 415 | Unsupported media type | The request media type is not supported | `urn:optrabidz:problem:unsupported-media-type` | `spring-mvc` |
| `VALIDATION_ERROR` | `TRANSPORT` | 400 | Request validation failed | One or more request values are invalid | `urn:optrabidz:problem:validation-error` | `spring-mvc` |
