package com.example.model;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.cassandra.mapping.Column;
import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.Table;

import com.example.model.primarykey.ReferenceDateTime;

@Table(value = "events_by_reference_and_year")
public class EventByReference extends Event {

  @Column(value = "event_type")
  protected String eventType;
  @PrimaryKey
  private ReferenceDateTime primaryKey;
  @Column(value = "correlation_id")
  private String correlationId;

  public ReferenceDateTime getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(final ReferenceDateTime primaryKey) {
    this.primaryKey = primaryKey;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(final String correlationId) {
    this.correlationId = correlationId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(final String eventType) {
    this.eventType = eventType;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("eventType", getEventType())
        .append("eventDateTime", getPrimaryKey().getEventDateTime())
        .append("correlationId", getCorrelationId())
        .append("clientIp", getClientIp())
        .append("userAgent", getUserAgent())
        .append("userAgentFiltered", getUserAgentFiltered())
        .append("details", getDetails())
        .append("reference", getPrimaryKey().getReference())
        .toString();
  }

}
