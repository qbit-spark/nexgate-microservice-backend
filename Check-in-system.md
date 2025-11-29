# Nexgate Check-in System - Technical Architecture

**Version:** 2.0  
**Last Updated:** November 29, 2025  
**Platform:** Nexgate Event Management

---

## Table of Contents

1. [What is This System?](#what-is-this-system)
2. [Core Concept](#core-concept)
3. [How It Works - Overview](#how-it-works---overview)
4. [System Components](#system-components)
5. [Ticket Flow](#ticket-flow)
6. [Scanner Registration](#scanner-registration)
7. [Ticket Validation](#ticket-validation)
8. [Offline Mode](#offline-mode)
9. [Security](#security)
10. [Database Design](#database-design)
11. [API Endpoints](#api-endpoints)
12. [Deployment](#deployment)

---

## What is This System?

A secure ticket validation system for events. Think of it like the scanners at concert gates or stadium entrances.

**Key Features:**
- Scan QR code tickets
- Works online and offline
- Prevents duplicate entry
- Supports multiple gates
- Cannot forge tickets

**Real-World Example:**
```
Customer buys ticket → Gets QR code via email
Customer arrives at venue → Shows QR code
Gate scanner scans QR → System validates → Entry granted or denied
```

---

## Core Concept

The system is built on **JWT (JSON Web Tokens)** with **RSA cryptographic signatures**.

### Why JWT + RSA?

```mermaid
graph LR
    A[Server has PRIVATE key] -->|Signs tickets| B[Ticket QR Code]
    C[Server has PUBLIC key] -->|Copies to| D[Scanner devices]
    B -->|Scanned by| E[Scanner]
    D -->|Verifies| E
    E -->|Valid or Invalid| F[Entry Decision]
    
    style A fill:#ff6666
    style D fill:#66ff66
```

**The Magic:**
- Server signs tickets with PRIVATE key (only server has this)
- Scanners verify tickets with PUBLIC key (everyone can have this)
- You can verify, but you cannot create signatures
- Like a government seal on a passport - you can check it's real, but you can't make one

**Result:**
- ✅ Cannot forge tickets (need private key)
- ✅ Cannot tamper with tickets (signature breaks)
- ✅ Can verify offline (just need public key)

---

## How It Works - Overview

```mermaid
flowchart TB
    subgraph "1. Setup"
        A1[Admin creates scanner registration QR]
        A2[Scanner scans QR and registers]
        A3[Scanner receives credentials + public key]
    end
    
    subgraph "2. Ticket Creation"
        B1[Customer buys ticket]
        B2[Server creates JWT signed with private key]
        B3[Customer receives QR code]
    end
    
    subgraph "3. Validation"
        C1[Customer shows QR at gate]
        C2[Scanner scans QR]
        C3[Scanner verifies JWT signature]
        C4[Online: Check duplicates with server]
        C5[Offline: Check duplicates locally]
        C6[Entry granted or denied]
    end
    
    A1 --> A2 --> A3
    B1 --> B2 --> B3
    B3 --> C1 --> C2 --> C3
    C3 --> C4
    C3 --> C5
    C4 --> C6
    C5 --> C6
```

---

## System Components

### Architecture Diagram

```mermaid
graph TB
    subgraph "Client Apps"
        SA[Scanner App<br/>Android]
        AD[Admin Dashboard<br/>Web]
        CA[Customer App<br/>Mobile/Web]
    end
    
    subgraph "Backend Services"
        API[Check-in Service<br/>Spring Boot]
        TS[Ticketing Service<br/>Spring Boot]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Database)]
        RD[(Redis<br/>Cache)]
    end
    
    SA -->|Validate tickets| API
    AD -->|Manage scanners| API
    CA -->|Buy tickets| TS
    TS -->|Generate tickets| API
    API --> PG
    API --> RD
    TS --> PG
    
    style API fill:#ff9900
    style SA fill:#ff9900
```

### Component Responsibilities

#### 1. Check-in Service (Backend)
**What it does:**
- Generates scanner registration QR codes
- Registers scanner devices
- Creates ticket JWTs with signatures
- Validates tickets online
- Receives offline scan logs
- Manages scanner settings

**Technology:**
- Spring Boot 3.x
- PostgreSQL
- Redis
- JWT library (io.jsonwebtoken)

#### 2. Scanner App (Android)
**What it does:**
- Registers using QR code
- Scans ticket QR codes
- Validates tickets (online or offline)
- Stores scan logs locally
- Syncs with server periodically

**Technology:**
- Android (Kotlin/Java)
- Room Database (local storage)
- ZXing (QR scanner)
- JWT verification

#### 3. Admin Dashboard (Web)
**What it does:**
- Create scanner registration QR codes
- View all scanners
- Revoke scanner access
- View scan statistics
- Monitor real-time activity

**Technology:**
- React/Vue.js
- WebSocket for real-time updates

---

---

## Event Creation with Key Generation

### How Events Are Created

When an admin creates an event, RSA keys are automatically generated and stored with the event.

```mermaid
sequenceDiagram
    participant Admin
    participant API
    participant EventService
    participant RSAKeyService
    participant Database
    
    Admin->>API: POST /api/events/create
    API->>EventService: createEvent(request)
    
    EventService->>RSAKeyService: generateKeys()
    RSAKeyService->>RSAKeyService: Generate RSA-2048 key pair
    RSAKeyService->>RSAKeyService: Encrypt private key (AES-256-GCM)
    RSAKeyService-->>EventService: Return RSAKeys object
    
    EventService->>EventService: Set event.rsaKeys = keys
    EventService->>Database: Save event (includes keys in JSONB)
    
    Database-->>EventService: Event saved
    EventService-->>API: Event created
    API-->>Admin: Success + Public Key
```

### Event Creation Request

```http
POST /api/events/create

Request:
{
  "name": "Tech Conference 2025",
  "eventDate": "2025-12-01T09:00:00Z",
  "venue": "Mlimani City Hall",
  "description": "Annual technology conference"
}

Response: 201 Created
{
  "id": "event-uuid-123",
  "name": "Tech Conference 2025",
  "eventDate": "2025-12-01T09:00:00Z",
  "venue": "Mlimani City Hall",
  "status": "DRAFT",
  "rsaKeys": {
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCg...",
    "algorithm": "RS256",
    "keySize": 2048,
    "status": "ACTIVE",
    "generatedAt": "2025-11-29T10:00:00Z"
  },
  "createdAt": "2025-11-29T10:00:00Z"
}
```

**What happens:**
1. Admin creates event
2. RSA-2048 key pair auto-generated (takes ~500ms)
3. Private key encrypted with AES-256-GCM
4. Keys saved in event's `rsa_keys` JSONB column
5. Event ready to sell tickets!

**Note:** Private key is encrypted and never exposed via API. Only public key is returned.

---

## Ticket Flow

### Step-by-Step: From Purchase to Entry

```mermaid
sequenceDiagram
    participant Admin
    participant Customer
    participant Ticketing
    participant CheckIn
    participant Scanner
    
    Note over Admin: First, create event with keys
    Admin->>CheckIn: Create Event
    CheckIn->>CheckIn: Generate RSA key pair
    CheckIn->>CheckIn: Save event with keys in JSONB
    
    Note over Customer: Customer purchases ticket
    Customer->>Ticketing: Buy ticket
    Ticketing->>CheckIn: Request ticket generation
    
    Note over CheckIn: 1. Load event (includes keys)<br/>2. Sign JWT with event's private key<br/>3. Generate QR code
    
    CheckIn->>Ticketing: Return JWT + QR
    Ticketing->>Customer: Email QR code
    
    Note over Customer: Customer arrives at venue
    
    Customer->>Scanner: Show QR code
    Scanner->>Scanner: Scan QR, extract JWT
    Scanner->>Scanner: Get event's public key from settings
    Scanner->>Scanner: Verify signature with public key
    
    alt Online Mode
        Scanner->>CheckIn: Check if ticket already used
        CheckIn->>Scanner: Valid / Duplicate / Invalid
    else Offline Mode
        Scanner->>Scanner: Check local database
    end
    
    Scanner->>Customer: Entry Granted / Denied
```

### Ticket Structure

**JWT Payload:**
```json
{
  "ticketId": "ticket-abc-123",
  "eventId": "event-xyz-789",
  "eventName": "Tech Conference 2025",
  "attendeeName": "John Doe",
  "attendeeEmail": "john@example.com",
  "ticketType": "VIP",
  "seatNumber": "A-15",
  "iat": 1701234567,
  "exp": 1703826567
}
```

**Complete JWT:**
```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0aWNrZXRJZCI6InRpY2tldC1hYmMtMTIzIiwiZXZlbnRJZCI6ImV2ZW50LXh5ei03ODkiLCJhdHRlbmRlZU5hbWUiOiJKb2huIERvZSIsImlhdCI6MTcwMTIzNDU2NywiZXhwIjoxNzAzODI2NTY3fQ.signature_here
```

This JWT is encoded into QR code.

---

## Scanner Registration

### Why QR-Based Registration?

Like WhatsApp's "Link Device" feature:
- Admin generates time-limited QR code (expires in 5 minutes)
- Scanner scans QR code
- Scanner receives credentials and public key
- Scanner is now authorized

### Registration Flow

**Key Concept:** Each scanner connects to ONE event at a time. To scan a different event, close the current session and register for the new event.

```mermaid
sequenceDiagram
    participant Admin
    participant Dashboard
    participant Server
    participant Scanner
    
    Note over Admin: Admin wants scanner for Event A
    
    Admin->>Dashboard: Click "Add Scanner"
    Admin->>Dashboard: Select Event A + Enter Gate Name
    Dashboard->>Server: Generate registration token for Event A
    
    Note over Server: Create token linked to Event A<br/>Set expiry: now + 5 minutes
    
    Server->>Dashboard: Return token
    Dashboard->>Dashboard: Generate QR code
    Dashboard->>Admin: Display QR
    
    Note over Admin,Scanner: Admin scans QR with scanner device
    
    Scanner->>Scanner: Extract token from QR
    Scanner->>Scanner: Compute device fingerprint
    
    Scanner->>Server: Register<br/>{token, name, fingerprint}
    
    Server->>Server: Validate token<br/>Get Event A details
    
    alt Token Valid
        Server->>Server: Generate scanner credentials (JWT)
        Server->>Server: Mark token as used
        Server->>Scanner: Return:<br/>- Scanner credentials<br/>- Event A public key<br/>- Event A details (name, date)
        Scanner->>Scanner: Clear any previous session
        Scanner->>Scanner: Store new session:<br/>- Event: "Tech Conference 2025"<br/>- Public key for Event A<br/>- Gate: "Gate A"
        Scanner->>Admin: ✅ Connected to Event A
    else Token Invalid
        Server->>Scanner: ❌ Invalid/expired token
        Scanner->>Admin: Registration failed
    end
    
    Note over Scanner: Scanner now validates<br/>ONLY Event A tickets
    
    Note over Scanner: To switch to Event B:<br/>1. Close session<br/>2. Scan new QR for Event B
```

**Session Management:**
- Scanner stores ONE active session at a time
- Session includes: event details, public key, gate name, credentials
- To switch events: Close session → Scan new QR → New session established
- Old event data is cleared when new session starts
- Each scanner-event connection is tracked separately in database

### What Scanner Receives and Stores

**During Registration, Scanner Receives:**

**1. Scanner Credentials (JWT):**
```json
{
  "scannerId": "scanner-uuid-123",
  "scannerName": "Gate A - Main Entrance",
  "eventId": "event-concert-123",
  "type": "scanner_credential",
  "iat": 1701234567,
  "exp": 1732770567
}
```
Expires in 1 year. Used to authenticate with server.

**2. Event Public Key:**
```
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
```
The specific public key for THIS event. Used to verify ticket signatures offline.

**3. Event Details:**
```json
{
  "eventId": "event-concert-123",
  "eventName": "Tech Conference 2025",
  "eventDate": "2025-12-01T09:00:00Z",
  "venue": "Mlimani City Hall"
}
```

**4. Session Settings:**
```json
{
  "offlineModeEnabled": false,
  "syncIntervalMinutes": 15,
  "maxOfflineHours": 24
}
```

**During Registration, Scanner Sends:**

**Device Fingerprint:**
```json
{
  "hardwareId": "sha256-hash-of-hardware-properties",
  "appSignature": "sha256-hash-of-app-signature"
}
```
This fingerprint is **STORED** on server and used for future validation.

**During Registration, Server Stores:**
```sql
INSERT INTO scanners (
    scanner_id,
    name,
    event_id,               -- ← Linked to specific event
    credentials,
    device_fingerprint,     -- ← STORED HERE
    status
) VALUES (
    'scanner-uuid-123',
    'Gate A - Main Entrance',
    'event-concert-123',    -- ← This scanner is for Event A only
    'eyJhbGc...',
    'ABC123',               -- ← Device fingerprint
    'ACTIVE'
);
```

**Scanner Local Storage (ONE session at a time):**
```sql
-- Scanner app database (SQLite)
CREATE TABLE active_session (
    id INTEGER PRIMARY KEY CHECK (id = 1),  -- Only ONE row!
    scanner_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    event_name VARCHAR(200) NOT NULL,
    gate_name VARCHAR(200),
    public_key TEXT NOT NULL,
    credentials TEXT NOT NULL,
    connected_at TIMESTAMP NOT NULL
);

-- Only one active session stored
-- When switching events: DELETE old, INSERT new
```

### Registration Token Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Generated: Admin creates
    Generated --> Active: Token valid
    Active --> Used: Scanner registers
    Active --> Expired: 5 minutes pass
    Used --> [*]: Archived
    Expired --> Deleted: Cleanup job (7 days)
    Deleted --> [*]
```

---

## Ticket Validation

### Online Validation (Default)

**How it works:**
1. Scanner scans QR code
2. Scanner extracts JWT from QR
3. **Scanner computes device fingerprint FRESH**
4. Scanner sends JWT + fingerprint to server
5. Server verifies:
    - Scanner credentials valid?
    - **Device fingerprint matches stored fingerprint?**
    - JWT signature valid?
    - JWT not expired?
    - Ticket not already scanned?
6. Server responds: Valid / Duplicate / Invalid
7. Scanner shows result to attendant

```mermaid
flowchart TD
    A[Scan QR Code] --> B[Extract JWT]
    B --> C[Compute Fresh Fingerprint]
    C --> D[Send to Server:<br/>JWT + Fingerprint]
    D --> E{Verify Scanner<br/>Credentials}
    E -->|Invalid| F[❌ Unauthorized Scanner]
    E -->|Valid| G{Check Fingerprint:<br/>Received == Stored?}
    G -->|Mismatch| H[❌ Device Mismatch<br/>Scanner Locked<br/>Alert Admin]
    G -->|Match| I{Verify JWT Signature}
    I -->|Invalid| J[❌ Forged Ticket]
    I -->|Valid| K{Check Expiry}
    K -->|Expired| L[❌ Expired Ticket]
    K -->|Valid| M{Check Redis Cache}
    M -->|Already Scanned| N[❌ Duplicate Entry]
    M -->|First Scan| O[✅ Entry Granted]
    O --> P[Add to Redis]
    O --> Q[Log to Database]
```

### Offline Validation (Emergency)

**When scanner has no internet:**
1. Scanner scans QR code
2. Scanner extracts JWT
3. Scanner verifies signature using stored public key
4. Scanner checks expiry from JWT
5. Scanner checks local database for duplicates
6. Scanner shows result
7. Scanner queues scan for later sync

```mermaid
flowchart TD
    A[Scan QR Code] --> B[Extract JWT]
    B --> C{Verify Signature<br/>with Public Key}
    C -->|Invalid| D[❌ Invalid Ticket]
    C -->|Valid| E{Check Expiry}
    E -->|Expired| F[❌ Expired Ticket]
    E -->|Valid| G{Check Local DB<br/>for Duplicates}
    G -->|Already Scanned| H[❌ Duplicate Entry<br/>at this gate]
    G -->|First Scan| I[✅ Entry Granted<br/>OFFLINE MODE]
    I --> J[Save to Local DB]
    I --> K[Queue for Sync]
```

**Limitation:** In offline mode, scanner can only detect duplicates at its own gate, not across other gates.

---

## Offline Mode

### Why Offline Mode?

Events happen in places with poor connectivity:
- Rural areas
- Basements/underground venues
- High traffic (network congestion)
- Emergency situations

System must work without internet.

### How Offline Mode Works

```mermaid
graph TB
    subgraph "Scanner Device"
        A[Stored Public Key] --> B[JWT Verification]
        C[Local SQLite DB] --> D[Duplicate Check]
        E[Sync Queue] --> F[Pending Scans]
    end
    
    subgraph "When Online Returns"
        F --> G[Send to Server]
        G --> H[Server Reconciles]
        H --> I[Detect Cross-Gate Duplicates]
        I --> J[Update Scanner Settings]
    end
```

### The Duplicate Problem

```mermaid
sequenceDiagram
    participant Gate_A
    participant Gate_B
    participant Server
    
    Note over Gate_A,Gate_B: Both scanners OFFLINE
    
    Gate_A->>Gate_A: Scan Ticket ABC<br/>✅ Valid (first at Gate A)
    Gate_B->>Gate_B: Scan Ticket ABC<br/>✅ Valid (Gate B doesn't know)
    
    Note over Gate_A,Gate_B: PROBLEM: Same ticket used twice!
    
    Note over Gate_A,Gate_B: Connection restored
    
    Gate_A->>Server: Sync: Scanned ABC at 10:00 AM
    Gate_B->>Server: Sync: Scanned ABC at 10:05 AM
    
    Server->>Server: Detect duplicate!<br/>ABC scanned at both gates
    Server->>Server: Generate alert
    Server->>Gate_B: Flag as suspicious scan
```

### Sync Process

**When scanner syncs:**
1. Scanner sends all pending scans to server
2. Server checks each scan:
    - Already in database? → Mark as DUPLICATE
    - New scan? → Record as SUCCESS
3. Server sends back:
    - Sync results
    - Updated settings
    - Any alerts
4. Scanner updates local database
5. Scanner applies new settings

**Sync triggers:**
- Every 15 minutes (default)
- When connection restored
- When app opens
- Manual sync button

---

## Security

### Security Architecture

```mermaid
graph TB
    subgraph "Event 1"
        E1[Event: Concert<br/>rsa_keys JSONB]
        E1PK[Private Key 1<br/>🔒 Encrypted]
        E1PUB[Public Key 1<br/>✅ Shareable]
    end
    
    subgraph "Event 2"
        E2[Event: Conference<br/>rsa_keys JSONB]
        E2PK[Private Key 2<br/>🔒 Encrypted]
        E2PUB[Public Key 2<br/>✅ Shareable]
    end
    
    subgraph "Tickets"
        T1[Concert Ticket 1]
        T2[Concert Ticket 2]
        T3[Conference Ticket 1]
    end
    
    subgraph "Scanners"
        S1[Concert Scanner<br/>Has Public Key 1]
        S2[Conference Scanner<br/>Has Public Key 2]
        S3[Multi-Event Scanner<br/>Has Both Keys]
    end
    
    E1 --> E1PK
    E1 --> E1PUB
    E2 --> E2PK
    E2 --> E2PUB
    
    E1PK -->|Signs| T1
    E1PK -->|Signs| T2
    E2PK -->|Signs| T3
    
    E1PUB -->|Copy to| S1
    E1PUB -->|Copy to| S3
    E2PUB -->|Copy to| S2
    E2PUB -->|Copy to| S3
    
    T1 -->|Scanned by| S1
    T2 -->|Scanned by| S3
    T3 -->|Scanned by| S2
    
    S1 -->|Verifies with| E1PUB
    S2 -->|Verifies with| E2PUB
    S3 -->|Verifies with| E1PUB
    S3 -->|Verifies with| E2PUB
```

**Key Points:**
- Each event has its own RSA key pair stored in `rsa_keys` JSONB column
- Private key is encrypted with AES-256-GCM before storage
- Public key is given to scanners during registration
- Scanners can validate tickets for their assigned events only
- If Event 1 key is compromised, Event 2 tickets remain secure

### Dynamic Device Fingerprinting

**Problem:** Attacker could copy scanner credentials to another device.

**Solution:** Device fingerprint computed fresh on every request and compared to stored fingerprint from registration.

#### How It Works

**Step 1: Registration (One Time)**
```
1. Scanner computes fingerprint from device hardware/software
2. Scanner sends fingerprint to server during registration
3. Server STORES fingerprint in database
4. This becomes the "expected" fingerprint for this scanner
```

**Step 2: Every Validation Request**
```
1. Scanner computes fingerprint FRESH (same algorithm)
2. Scanner sends fingerprint with ticket validation request
3. Server retrieves stored fingerprint from database
4. Server compares: received == stored?
   - MATCH → Continue with validation
   - MISMATCH → Reject request, lock scanner, alert admin
```

**Fingerprint Components:**
- Hardware: Android ID, serial number, build fingerprint
- Display: Screen resolution, density, DPI
- Components: CPU architecture, sensor list, battery capacity
- App: Signature, install timestamp, installer source
- Combined and hashed → SHA-256 → Unique device signature

#### Visual Flow

```mermaid
sequenceDiagram
    participant Scanner_Real as Real Scanner
    participant Scanner_Fake as Attacker Device
    participant Server
    participant Database
    
    rect rgb(1, 2, 200)
        Note over Scanner_Real,Database: REGISTRATION (Once)
        
        Scanner_Real->>Scanner_Real: Compute fingerprint<br/>hash(Samsung S21 specs)
        Note over Scanner_Real: Result: "ABC123"
        
        Scanner_Real->>Server: Register<br/>{token, fingerprint: "ABC123"}
        Server->>Database: STORE fingerprint = "ABC123"
        Server->>Scanner_Real: ✅ Registered
    end

    rect rgb(2, 100, 10)
        Note over Scanner_Real,Database: NORMAL VALIDATION
        
        Scanner_Real->>Scanner_Real: Compute fingerprint FRESH<br/>hash(Samsung S21 specs)
        Note over Scanner_Real: Result: "ABC123"
        
        Scanner_Real->>Server: Validate ticket<br/>{jwt, fingerprint: "ABC123"}
        Server->>Database: Get stored fingerprint
        Database->>Server: "ABC123"
        Server->>Server: Compare: "ABC123" == "ABC123"
        Server->>Scanner_Real: ✅ MATCH - Proceed
    end

    rect rgb(255, 100, 10)
        Note over Scanner_Fake,Database: ATTACK SCENARIO
        
        Note over Scanner_Fake: Attacker copied credentials<br/>to different device
        
        Scanner_Fake->>Scanner_Fake: Compute fingerprint<br/>hash(Generic Phone specs)
        Note over Scanner_Fake: Result: "XYZ789"
        
        Scanner_Fake->>Server: Validate ticket<br/>{jwt, fingerprint: "XYZ789"}
        Server->>Database: Get stored fingerprint
        Database->>Server: "ABC123"
        Server->>Server: Compare: "XYZ789" == "ABC123"
        Server->>Server: ❌ MISMATCH! Different device!
        Server->>Server: 🚨 Revoke scanner<br/>Alert admin
        Server->>Scanner_Fake: ❌ UNAUTHORIZED
    end
```

#### Why This Works

**Stored Fingerprint (Registration):**
```
Database: scanners table
┌─────────────────────────────────┐
│ scanner_id: "scanner-001"       │
│ device_fingerprint: "ABC123"    │ ← Stored once during registration
│ name: "Gate A Scanner"          │
└─────────────────────────────────┘
```

**Fresh Fingerprint (Every Request):**
```
Validation Request Payload
┌─────────────────────────────────┐
│ scannerId: "scanner-001"        │
│ ticketJWT: "eyJhbGc..."         │
│ fingerprint: "ABC123"           │ ← Computed fresh from device
└─────────────────────────────────┘
```

**Server Comparison:**
```
stored "ABC123" == received "ABC123" → ✅ Same device (continue)
stored "ABC123" == received "XYZ789" → ❌ Different device (block!)
```

**Attack Resistance:**
- Attacker copies credentials → Still blocked
- Different phone = Different hardware = Different fingerprint
- Server detects mismatch immediately
- Scanner revoked automatically
- Admin receives security alert

### Attack Scenarios

#### 1. Ticket Forgery
```
Attack: Create fake ticket JWT
Defense: Cannot sign without private key
Result: ❌ Signature verification fails
```

#### 2. Ticket Tampering
```
Attack: Modify ticket data (change seat, upgrade type)
Defense: Any change breaks signature
Result: ❌ Signature verification fails
```

#### 3. Credential Theft
```
Attack: Copy scanner credentials to another device
Defense: Device fingerprint mismatch
Result: ❌ Request denied, scanner locked, admin alerted
```

#### 4. Ticket Cloning (Online)
```
Attack: Share same QR code with friend
Defense: Redis tracks all scans
Result: ✅ First scan works, second scan rejected
```

#### 5. Ticket Cloning (Offline)
```
Attack: Use same QR at different gates while offline
Defense: Limited - detected during sync
Result: ⚠️ Both scans succeed initially, flagged during sync
```

### Security Best Practices

**Server Side:**
- Private key encrypted at rest
- Private key never exposed via API
- Regular key rotation supported
- All API requests require authentication
- Rate limiting on all endpoints

**Scanner Side:**
- Credentials stored in Android Keystore (hardware-backed)
- Cannot extract private data even with root
- Device fingerprint computed, never static
- Session tokens expire after 4 hours
- Local database encrypted

---

## Database Design

### Entity Relationship Diagram

```mermaid
erDiagram
    REGISTRATION_TOKENS ||--o| SCANNERS : "registers"
    SCANNERS ||--o{ TICKET_SCANS : "performs"
    TICKETS ||--o{ TICKET_SCANS : "scanned_as"
    EVENTS ||--o{ TICKETS : "contains"
    
    REGISTRATION_TOKENS {
        uuid id PK
        string token UK
        timestamp expires_at
        boolean used
        timestamp used_at
        string created_by
        int validity_minutes
    }
    
    SCANNERS {
        uuid id PK
        string scanner_id UK
        string name
        text credentials
        string status
        string device_fingerprint
        jsonb settings
        timestamp last_synced_at
    }
    
    TICKETS {
        uuid id PK
        string ticket_id UK
        uuid event_id FK
        string attendee_name
        text jwt_token
        text qr_code_base64
        string status
        timestamp valid_from
        timestamp valid_until
    }
    
    TICKET_SCANS {
        uuid id PK
        string ticket_id FK
        uuid scanner_id FK
        timestamp scanned_at
        string validation_mode
        string scan_result
    }
    
    EVENTS {
        uuid id PK
        string name
        timestamp event_date
        string venue
        jsonb rsa_keys
    }
```

### Table Details

#### registration_tokens
```sql
CREATE TABLE registration_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(100) UNIQUE NOT NULL,
    event_id VARCHAR(100) NOT NULL,  -- Token is for specific event
    gate_name VARCHAR(200),           -- Optional gate/location name
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    created_by VARCHAR(100),
    validity_minutes INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE INDEX idx_token ON registration_tokens(token);
CREATE INDEX idx_event_id ON registration_tokens(event_id);
CREATE INDEX idx_valid_tokens ON registration_tokens(used, expires_at) 
    WHERE used = FALSE;
```

**Purpose:** Time-limited tokens for scanner registration, each linked to a specific event  
**Lifecycle:** Created → Used (or Expired) → Deleted after 7 days

#### scanners
```sql
CREATE TABLE scanners (
    id UUID PRIMARY KEY,
    scanner_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    event_id VARCHAR(100) NOT NULL,  -- Linked to specific event
    credentials TEXT NOT NULL,
    status VARCHAR(20) NOT NULL, -- ACTIVE, CLOSED, REVOKED
    device_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP,
    last_seen_ip VARCHAR(45),
    last_seen_location JSONB,
    closed_at TIMESTAMP,
    
    FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE INDEX idx_scanner_id ON scanners(scanner_id);
CREATE INDEX idx_scanner_event ON scanners(event_id);
CREATE INDEX idx_scanner_status ON scanners(status);
CREATE INDEX idx_device_fingerprint ON scanners(device_fingerprint);
```

**Purpose:** Registered scanner devices (one scanner per event connection)  
**Status values:**
- `ACTIVE` - Currently connected and scanning
- `CLOSED` - Session closed (scanner switched to different event)
- `REVOKED` - Blocked due to security violation

**Note:** Same physical device can have multiple scanner records (one per event it connects to), but only ONE active session at a time on the device.

#### events
```sql
CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    venue VARCHAR(200),
    status VARCHAR(20) DEFAULT 'DRAFT',
    rsa_keys JSONB,  -- RSA key pair stored as JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_rsa_status ON events((rsa_keys->>'status'));
```

**Purpose:** Events with embedded RSA keys  
**RSA Keys JSONB format:**
```json
{
  "privateKey": "encrypted-base64-encoded-private-key",
  "publicKey": "base64-encoded-public-key",
  "algorithm": "RS256",
  "keySize": 2048,
  "status": "ACTIVE",
  "generatedAt": "2025-11-29T10:00:00Z"
}
```

**Why JSONB?**
- ✅ Keys belong to event (logical grouping)
- ✅ Atomic operations (create event + keys together)
- ✅ Automatic cleanup (delete event = delete keys)
- ✅ No joins needed
- ✅ Simpler code

#### tickets
```sql
CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    ticket_id VARCHAR(100) UNIQUE NOT NULL,
    event_id UUID NOT NULL,
    attendee_name VARCHAR(200) NOT NULL,
    attendee_email VARCHAR(200),
    ticket_type VARCHAR(50),
    jwt_token TEXT NOT NULL,
    qr_code_base64 TEXT,
    status VARCHAR(20) NOT NULL, -- ACTIVE, SCANNED, CANCELLED
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE INDEX idx_ticket_id ON tickets(ticket_id);
CREATE INDEX idx_event_id ON tickets(event_id);
CREATE INDEX idx_status ON tickets(status);
```

**Purpose:** Event tickets with JWT and QR code

#### ticket_scans
```sql
CREATE TABLE ticket_scans (
    id UUID PRIMARY KEY,
    ticket_id VARCHAR(100) NOT NULL,
    scanner_id UUID NOT NULL,
    scanned_at TIMESTAMP NOT NULL,
    validation_mode VARCHAR(20) NOT NULL, -- ONLINE, OFFLINE
    scan_result VARCHAR(20) NOT NULL, -- SUCCESS, DUPLICATE, INVALID
    synced_at TIMESTAMP,
    
    FOREIGN KEY (scanner_id) REFERENCES scanners(id)
);

CREATE INDEX idx_ticket_scans_ticket ON ticket_scans(ticket_id);
CREATE INDEX idx_ticket_scans_scanner ON ticket_scans(scanner_id);
CREATE INDEX idx_ticket_scans_time ON ticket_scans(scanned_at);
```

**Purpose:** Audit log of all scan attempts

### Scanner Local Database (SQLite)

```sql
-- Scanner configuration
CREATE TABLE scanner_config (
    key VARCHAR(50) PRIMARY KEY,
    value TEXT NOT NULL
);

-- Locally scanned tickets
CREATE TABLE scanned_tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id VARCHAR(100) UNIQUE NOT NULL,
    scanned_at TIMESTAMP NOT NULL,
    attendee_name VARCHAR(200),
    validation_result VARCHAR(20) NOT NULL
);

-- Pending sync queue
CREATE TABLE sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id VARCHAR(100) NOT NULL,
    scanned_at TIMESTAMP NOT NULL,
    validation_mode VARCHAR(20) NOT NULL,
    synced BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sync_pending ON sync_queue(synced) WHERE synced = FALSE;
```

---

## API Endpoints

### Base URL
```
Production: https://api.nexgate.com/v1
Staging: https://staging-api.nexgate.com/v1
```

### Authentication
All requests require JWT in header:
```
Authorization: Bearer {scanner_credentials_jwt}
```

---

### 1. Generate Registration Token

```http
POST /api/registration-tokens/generate
Authorization: Bearer {admin_jwt}

Request:
{
  "validityMinutes": 5,
  "notes": "Gate A scanner"
}

Response: 201 Created
{
  "token": "abc-123-xyz-789",
  "qrCodeBase64": "data:image/png;base64,iVBORw0KG...",
  "expiresAt": "2025-11-29T10:15:00Z",
  "validityMinutes": 5
}
```

---

### 2. Register Scanner

```http
POST /api/scanners/register

Request:
{
  "token": "abc-123-xyz-789",
  "deviceName": "Gate A Scanner",
  "deviceFingerprint": {
    "hardwareId": "computed-hash-of-hardware",
    "appSignature": "app-signature-hash"
  }
}

Response: 201 Created
{
  "scannerId": "scanner-uuid-123",
  "credentials": "eyJhbGc...scanner_jwt",
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ...",
  "settings": {
    "offlineModeEnabled": false,
    "syncIntervalMinutes": 15
  }
}

Errors:
400 - Invalid or expired token
409 - Token already used
```

---

### 3. Validate Ticket

```http
POST /api/tickets/validate
Authorization: Bearer {scanner_credentials}

Request:
{
  "jwt": "eyJhbGc...ticket_jwt",
  "scannerId": "scanner-uuid-123",
  "deviceFingerprint": {
    "hardwareId": "computed-hash",
    "appSignature": "app-hash"
  }
}

Response: 200 OK (Valid)
{
  "valid": true,
  "ticketId": "ticket-abc-123",
  "attendeeName": "John Doe",
  "eventName": "Tech Conference 2025",
  "ticketType": "VIP"
}

Response: 400 Bad Request (Duplicate)
{
  "valid": false,
  "reason": "DUPLICATE",
  "message": "Ticket already scanned",
  "originalScanTime": "2025-11-29T10:25:00Z",
  "originalScanner": "Gate B Scanner"
}

Response: 403 Forbidden (Fingerprint Mismatch)
{
  "valid": false,
  "reason": "DEVICE_MISMATCH",
  "message": "Device fingerprint does not match. Scanner locked.",
  "action": "CONTACT_ADMIN"
}
```

---

### 4. Sync Scanner

```http
POST /api/scanners/sync
Authorization: Bearer {scanner_credentials}

Request:
{
  "scannerId": "scanner-uuid-123",
  "scans": [
    {
      "ticketId": "ticket-abc-123",
      "scannedAt": "2025-11-29T10:05:00Z",
      "validationMode": "OFFLINE"
    },
    {
      "ticketId": "ticket-xyz-456",
      "scannedAt": "2025-11-29T10:10:00Z",
      "validationMode": "OFFLINE"
    }
  ],
  "lastSyncAt": "2025-11-29T09:00:00Z"
}

Response: 200 OK
{
  "syncResults": [
    {
      "ticketId": "ticket-abc-123",
      "status": "SUCCESS"
    },
    {
      "ticketId": "ticket-xyz-456",
      "status": "DUPLICATE",
      "message": "Already scanned at Gate B",
      "originalScanTime": "2025-11-29T10:08:00Z"
    }
  ],
  "settings": {
    "offlineModeEnabled": true,
    "syncIntervalMinutes": 15
  },
  "serverTime": "2025-11-29T10:30:00Z"
}
```

---

### 5. Revoke Scanner

```http
POST /api/scanners/{scannerId}/revoke
Authorization: Bearer {admin_jwt}

Response: 200 OK
{
  "scannerId": "scanner-uuid-123",
  "status": "REVOKED",
  "revokedAt": "2025-11-29T10:30:00Z"
}
```

---

## Deployment

### Infrastructure

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[AWS ALB<br/>SSL/TLS]
    end
    
    subgraph "Application Layer"
        API1[Check-in Service 1]
        API2[Check-in Service 2]
        API3[Check-in Service 3]
    end
    
    subgraph "Cache"
        REDIS[(Redis Cluster<br/>3 nodes)]
    end
    
    subgraph "Database"
        PG_M[(PostgreSQL Primary)]
        PG_R[(PostgreSQL Replica)]
    end
    
    subgraph "Storage"
        S3[(S3 Bucket<br/>QR Codes)]
    end
    
    LB --> API1
    LB --> API2
    LB --> API3
    
    API1 --> REDIS
    API2 --> REDIS
    API3 --> REDIS
    
    API1 --> PG_M
    API2 --> PG_M
    API3 --> PG_M
    
    PG_M -->|Replication| PG_R
    
    API1 --> S3
    API2 --> S3
    API3 --> S3
```

### Production Configuration

**Application:**
- Instances: 3 (Auto-scaling: 2-10)
- Instance Type: t3.large (2 vCPU, 8 GB RAM)
- Container: Docker via ECS

**Database:**
- Type: Amazon RDS PostgreSQL 15
- Instance: db.r6g.xlarge (4 vCPU, 32 GB RAM)
- Storage: 500 GB SSD
- Backups: Daily, 30-day retention
- Replication: 1 read replica

**Cache:**
- Type: Amazon ElastiCache Redis
- Node Type: cache.r6g.large
- Cluster: 3 nodes (1 primary, 2 replicas)

**Load Balancer:**
- Type: Application Load Balancer
- SSL: AWS Certificate Manager
- Health checks: /api/health every 30s

---

## Glossary

**JWT (JSON Web Token):** A compact token format for securely transmitting information between parties.

**RSA:** Asymmetric encryption algorithm using public/private key pairs.

**QR Code:** 2D barcode that stores data, readable by camera.

**Device Fingerprint:** Unique identifier computed from device hardware and software properties.

**Offline Mode:** Scanner operation without internet connectivity.

**Sync:** Process of sending local scan data to server.

**Duplicate Scan:** Attempting to scan the same ticket multiple times.

**Challenge-Response:** Authentication method where server sends challenge and device must prove identity.

---

**Document Status:** Production Ready  
**Maintained By:** Nexgate Platform Team  
**Contact:** dev@nexgate.com

---