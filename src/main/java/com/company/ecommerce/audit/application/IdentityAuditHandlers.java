package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.auth.domain.event.UserLoggedInEvent;
import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.user.domain.event.AddressAddedEvent;
import com.company.ecommerce.user.domain.event.CustomerCreatedEvent;
import com.company.ecommerce.user.domain.event.CustomerUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records audit entries for auth and user (identity) events. */
@Component
@RequiredArgsConstructor
public class IdentityAuditHandlers {

    private final AuditLogWriter audit;

    @ApplicationModuleListener
    public void on(UserRegisteredEvent event) {
        audit.record(
                AuditCategory.AUTH,
                "UserRegistered",
                "REGISTER",
                "User",
                event.userId(),
                event.userId(),
                "User registered: " + event.email());
    }

    @ApplicationModuleListener
    public void on(UserLoggedInEvent event) {
        audit.record(
                AuditCategory.AUTH,
                "UserLoggedIn",
                "LOGIN",
                "User",
                event.userId(),
                event.userId(),
                "User logged in: " + event.email());
    }

    @ApplicationModuleListener
    public void on(CustomerCreatedEvent event) {
        audit.record(
                AuditCategory.USER,
                "CustomerCreated",
                "CREATE",
                "Customer",
                event.customerId(),
                event.userId(),
                "Customer profile created: " + event.email());
    }

    @ApplicationModuleListener
    public void on(CustomerUpdatedEvent event) {
        audit.record(
                AuditCategory.USER,
                "CustomerUpdated",
                "UPDATE",
                "Customer",
                event.customerId(),
                event.userId(),
                "Customer profile updated");
    }

    @ApplicationModuleListener
    public void on(AddressAddedEvent event) {
        audit.record(
                AuditCategory.USER,
                "AddressAdded",
                "CREATE",
                "Address",
                event.addressId(),
                event.customerId(),
                "Address added");
    }
}
