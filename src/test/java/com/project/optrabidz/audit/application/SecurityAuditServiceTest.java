package com.project.optrabidz.audit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.application.policy.AuditPolicyRegistry;
import com.project.optrabidz.audit.infrastructure.entity.AuditRecord;
import com.project.optrabidz.audit.infrastructure.repository.JpaAuditRecordRepository;
import com.project.optrabidz.common.observability.OperationalEventLogger;
import com.project.optrabidz.common.observability.SensitiveDataMasker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {
    @Mock
    private JpaAuditRecordRepository auditRecordRepository;
    @Mock
    private AuditPolicyRegistry auditPolicyRegistry;
    @Mock
    private OperationalEventLogger operationalEventLogger;

    @Test
    void webhookSecurityRecordsContainOnlyBoundedMetadata() {
        AuditService auditService = mock(AuditService.class);
        SensitiveDataMasker sensitiveDataMasker = new SensitiveDataMasker();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        SecurityAuditService service = new SecurityAuditService(
                auditService,
                new AuditRecordFactory(objectMapper, sensitiveDataMasker),
                sensitiveDataMasker,
                objectMapper,
                operationalEventLogger
        );

        service.recordPaymentWebhookRejected("UPI", "request-123");
        service.recordPaymentWebhookPayloadInvalid("UPI", "request-456");

        ArgumentCaptor<AuditRecord> records = ArgumentCaptor.forClass(
                AuditRecord.class
        );
        verify(auditService, times(2)).save(records.capture());
        assertThat(records.getAllValues())
                .extracting(AuditRecord::getAction)
                .containsExactly(
                        "PAYMENT_WEBHOOK_REJECTED",
                        "PAYMENT_WEBHOOK_PAYLOAD_INVALID"
                );
        assertThat(records.getAllValues())
                .allSatisfy(record -> {
                    assertThat(record.getObjectType()).isEqualTo(
                            "PAYMENT_PROVIDER"
                    );
                    assertThat(record.getObjectId()).isEqualTo("UPI");
                    assertThat(record.getOutcome()).isEqualTo("DENIED");
                    assertThat(record.getDetails()).isEqualTo(
                            "{\"category\":\"PAYMENT_WEBHOOK\"}"
                    );
                    assertThat(record.getIpAddress()).isNull();
                    assertThat(record.getUserAgent()).isNull();
                });
        assertThat(records.getAllValues())
                .extracting(AuditRecord::getRequestId)
                .containsExactly("request-123", "request-456");
    }

    @Test
    void webhookAuditFailureDoesNotEscape() {
        AuditService auditService = mock(AuditService.class);
        RuntimeException persistenceFailure = new IllegalStateException(
                "database unavailable: secret-token"
        );
        doThrow(persistenceFailure).when(auditService).save(any());
        SensitiveDataMasker sensitiveDataMasker = new SensitiveDataMasker();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        SecurityAuditService service = new SecurityAuditService(
                auditService,
                new AuditRecordFactory(objectMapper, sensitiveDataMasker),
                sensitiveDataMasker,
                objectMapper,
                operationalEventLogger
        );

        assertThatCode(() -> service.recordPaymentWebhookRejected(
                "UNKNOWN",
                "request-789"
        )).doesNotThrowAnyException();

        verify(operationalEventLogger).error(
                "SECURITY_AUDIT_WRITE_FAILED",
                "action=PAYMENT_WEBHOOK_REJECTED objectType=PAYMENT_PROVIDER "
                        + "objectId=UNKNOWN",
                persistenceFailure
        );
    }

    @Test
    void containsCommitFailuresAtBothSecurityBoundaries() throws Exception {
        assertTransactionalBoundary();
        RuntimeException persistenceFailure = new IllegalStateException(
                "database unavailable: secret-token"
        );
        SensitiveDataMasker sensitiveDataMasker = new SensitiveDataMasker();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder
                .json()
                .build();
        AuditRecordFactory auditRecordFactory = new AuditRecordFactory(
                objectMapper,
                sensitiveDataMasker
        );
        PlatformTransactionManager transactionManager = mock(
                PlatformTransactionManager.class
        );
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any()))
                .thenReturn(transactionStatus);
        doThrow(persistenceFailure)
                .when(transactionManager)
                .commit(transactionStatus);
        AuditService auditService = transactionalProxy(
                transactionManager,
                auditRecordFactory
        );
        SecurityAuditService service = new SecurityAuditService(
                auditService,
                auditRecordFactory,
                sensitiveDataMasker,
                objectMapper,
                operationalEventLogger
        );
        MockHttpServletRequest authenticationRequest = request(
                "GET",
                "/api/v1/me"
        );
        MockHttpServletRequest authorizationRequest = request(
                "POST",
                "/api/v1/auth/logout"
        );

        assertThatCode(() -> service.recordAuthenticationRequired(
                authenticationRequest,
                "AUTHENTICATION_REQUIRED"
        )).doesNotThrowAnyException();
        assertThatCode(() -> service.recordAuthorizationDenied(
                authorizationRequest,
                "CSRF_VALIDATION_FAILED",
                42L,
                "STARTUP"
        )).doesNotThrowAnyException();

        verify(auditRecordRepository, times(2)).save(any());
        verify(transactionManager, times(2)).commit(transactionStatus);
        InOrder failures = inOrder(operationalEventLogger);
        failures.verify(operationalEventLogger).error(
                "SECURITY_AUDIT_WRITE_FAILED",
                "action=AUTHENTICATION_REQUIRED "
                        + "objectType=HTTP_REQUEST objectId=/api/v1/me",
                persistenceFailure
        );
        failures.verify(operationalEventLogger).error(
                "SECURITY_AUDIT_WRITE_FAILED",
                "action=AUTHORIZATION_DENIED "
                        + "objectType=HTTP_REQUEST "
                        + "objectId=/api/v1/auth/logout",
                persistenceFailure
        );
        failures.verifyNoMoreInteractions();
    }

    private void assertTransactionalBoundary() throws Exception {
        Method authenticationMethod = SecurityAuditService.class
                .getMethod(
                        "recordAuthenticationRequired",
                        jakarta.servlet.http.HttpServletRequest.class,
                        String.class
                );
        Method loginFailureMethod = SecurityAuditService.class
                .getMethod(
                        "recordLoginFailure",
                        String.class,
                        String.class,
                        jakarta.servlet.http.HttpServletRequest.class
                );
        Method authorizationMethod = SecurityAuditService.class
                .getMethod(
                        "recordAuthorizationDenied",
                        jakarta.servlet.http.HttpServletRequest.class,
                        String.class,
                        Long.class,
                        String.class
                );
        Transactional saveTransaction = AuditService.class
                .getMethod(
                        "save",
                        AuditRecord.class
                )
                .getAnnotation(Transactional.class);

        assertThat(loginFailureMethod.getAnnotation(Transactional.class))
                .isNull();
        assertThat(authenticationMethod.getAnnotation(Transactional.class))
                .isNull();
        assertThat(authorizationMethod.getAnnotation(Transactional.class))
                .isNull();
        assertThat(saveTransaction).isNotNull();
        assertThat(saveTransaction.propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    private AuditService transactionalProxy(
            PlatformTransactionManager transactionManager,
            AuditRecordFactory auditRecordFactory
    ) {
        AuditService target = new AuditService(
                auditRecordRepository,
                auditRecordFactory,
                auditPolicyRegistry
        );
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(
                new AnnotationTransactionAttributeSource()
        );
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(interceptor);
        return (AuditService) proxyFactory.getProxy();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                method,
                path
        );
        request.addHeader("X-Request-Id", "audit-request-123");
        request.addHeader("User-Agent", "audit-test-client");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
