package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public class ReconciliationMovementRow {

    private String entryType;
    private String label;
    private BigDecimal pointsMagnitude;
    private BigDecimal signedPointsImpact;
    private long transactionCount;
    private long uniqueCustomers;
    private BigDecimal monetaryValue;

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getPointsMagnitude() { return pointsMagnitude; }
    public void setPointsMagnitude(BigDecimal pointsMagnitude) { this.pointsMagnitude = pointsMagnitude; }
    public BigDecimal getSignedPointsImpact() { return signedPointsImpact; }
    public void setSignedPointsImpact(BigDecimal signedPointsImpact) { this.signedPointsImpact = signedPointsImpact; }
    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }
    public long getUniqueCustomers() { return uniqueCustomers; }
    public void setUniqueCustomers(long uniqueCustomers) { this.uniqueCustomers = uniqueCustomers; }
    public BigDecimal getMonetaryValue() { return monetaryValue; }
    public void setMonetaryValue(BigDecimal monetaryValue) { this.monetaryValue = monetaryValue; }
}
