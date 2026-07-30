# Architecture and Workflow Diagrams

This document provides high-level system diagrams for CreatorOS Backend. Diagrams use Mermaid syntax so they can be rendered directly in Markdown viewers that support Mermaid.

## Architecture Diagram

```mermaid
flowchart LR
  subgraph Clients
    Web[Web/Mobile Clients]
    Admin[Admin/Ops]
  end

  Web --> Gateway[API Gateway]
  Admin --> Gateway

  subgraph Services
    Auth[Auth Service]
    Profile[Profile Service]
    Content[Content Service]
    Asset[Asset Service]
    Publishing[Publishing Service]
    Scheduler[Scheduler Service]
    Notification[Notification Service]
    Analytics[Analytics Service]
  end

  Gateway --> Auth
  Gateway --> Profile
  Gateway --> Content
  Gateway --> Asset
  Gateway --> Publishing
  Gateway --> Scheduler
  Gateway --> Notification
  Gateway --> Analytics

  subgraph DataStores
    AuthDB[(Auth DB)]
    ProfileDB[(Profile DB)]
    ContentDB[(Content DB)]
    AssetDB[(Asset DB)]
    PublishingDB[(Publishing DB)]
    SchedulerDB[(Scheduler DB)]
    NotificationDB[(Notification DB)]
    AnalyticsDB[(Analytics DB)]
    Redis[(Redis Cache)]
  end

  Auth --> AuthDB
  Profile --> ProfileDB
  Content --> ContentDB
  Asset --> AssetDB
  Publishing --> PublishingDB
  Scheduler --> SchedulerDB
  Notification --> NotificationDB
  Analytics --> AnalyticsDB

  Auth --> Redis
  Profile --> Redis
  Content --> Redis
  Asset --> Redis
  Publishing --> Redis
  Scheduler --> Redis
  Notification --> Redis
  Analytics --> Redis

  Kafka[(Kafka/Event Bus)]
  ZK[(Zookeeper)]

  Auth -- events --> Kafka
  Profile -- events --> Kafka
  Content -- events --> Kafka
  Asset -- events --> Kafka
  Publishing -- events --> Kafka
  Scheduler -- events --> Kafka
  Notification -- events --> Kafka
  Analytics -- events --> Kafka

  Kafka --- ZK
```

## Workflow Diagram (Publish Content)

```mermaid
sequenceDiagram
  participant Creator
  participant Gateway as API Gateway
  participant Auth as Auth Service
  participant Content as Content Service
  participant Asset as Asset Service
  participant Publishing as Publishing Service
  participant Scheduler as Scheduler Service
  participant Notification as Notification Service
  participant Analytics as Analytics Service

  Creator->>Gateway: Submit publish request
  Gateway->>Auth: Validate JWT
  Auth-->>Gateway: Auth OK
  Gateway->>Content: Create content record
  Content->>Asset: Store media metadata
  Asset-->>Content: Asset ID
  Content->>Publishing: Create publish job
  Publishing->>Scheduler: Schedule publish
  Scheduler-->>Publishing: Job ID
  Publishing->>Notification: Send confirmation
  Publishing->>Analytics: Emit publish event
  Notification-->>Creator: Delivery confirmation
```

## Use Case Diagram

```mermaid
usecaseDiagram
  actor Creator
  actor Admin

  Creator --> (Authenticate)
  Creator --> (Manage Profile)
  Creator --> (Create Content)
  Creator --> (Upload Assets)
  Creator --> (Publish Content)
  Creator --> (Schedule Posts)
  Creator --> (View Analytics)
  Creator --> (Receive Notifications)

  Admin --> (Manage Users)
  Admin --> (Monitor System Health)
  Admin --> (Review Event Streams)
```

## Object Diagram (Sample Instances)

```mermaid
classDiagram
  class CreatorUser {
    <<instance>>
    id = "user_42"
    email = "creator@example.com"
  }

  class ProfileRecord {
    <<instance>>
    displayName = "Studio Nova"
  }

  class ContentItem {
    <<instance>>
    status = "draft"
  }

  class AssetFile {
    <<instance>>
    type = "video/mp4"
  }

  class PublishJob {
    <<instance>>
    scheduledAt = "2026-05-01T10:00Z"
  }

  class NotificationMessage {
    <<instance>>
    channel = "email"
  }

  CreatorUser --> ProfileRecord : owns
  CreatorUser --> ContentItem : creates
  ContentItem --> AssetFile : uses
  ContentItem --> PublishJob : schedules
  PublishJob --> NotificationMessage : triggers
```

## ER Diagram

```mermaid
erDiagram
  USER {
    uuid id
    string email
    string status
  }

  PROFILE {
    uuid id
    uuid user_id
    string display_name
  }

  CONTENT {
    uuid id
    uuid user_id
    string status
  }

  ASSET {
    uuid id
    uuid content_id
    string storage_uri
  }

  PUBLISH_JOB {
    uuid id
    uuid content_id
    datetime scheduled_at
  }

  NOTIFICATION {
    uuid id
    uuid user_id
    string channel
  }

  ANALYTICS_EVENT {
    uuid id
    uuid content_id
    string event_type
  }

  USER ||--|| PROFILE : has
  USER ||--o{ CONTENT : creates
  CONTENT ||--o{ ASSET : uses
  CONTENT ||--o{ PUBLISH_JOB : schedules
  USER ||--o{ NOTIFICATION : receives
  CONTENT ||--o{ ANALYTICS_EVENT : emits
```
