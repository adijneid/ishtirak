package org.example.ishtirak.common.model;

/** Computed from payments, never stored (see proposal, normalization rationale). */
public enum InvoiceStatus {
    UNPAID,
    PARTIAL,
    PAID,
    VOID
}
