# CAHIER DES CHARGES & DOCUMENTATION DES API BACKEND ONCHAT

Document d'architecture et de référence des endpoints REST pour l'écosystème **OnChat & CashPay**. Ce document décrit les routes API, les structures de requêtes, les codes de statut HTTP et les exemples de réponses JSON pour l'implémentation du serveur d'arrière-plan.

---

## 1. AUTHENTIFICATION & PROFIL UTILISATEUR

### 1.1 Demande de Code OTP
* **Route :** `POST /api/v1/auth/request-otp`
* **Description :** Génère et envoie un code d'authentification à 6 chiffres sur le numéro du téléphone ou l'identifiant CashPay.
* **Headers :** `Content-Type: application/json`
* **Body Request :**
```json
{
  "phoneOrWalletId": "+243810000000"
}
```
* **Response 200 OK :**
```json
{
  "success": true,
  "message": "Code OTP transmis avec succès",
  "otpSessionId": "otp_sess_9823471092"
}
```

### 1.2 Vérification OTP & Génération de Token Session
* **Route :** `POST /api/v1/auth/verify-otp`
* **Description :** Valide le code à 6 chiffres et retourne un token d'accès JWT sécurisé ainsi que le profil utilisateur.
* **Body Request :**
```json
{
  "otpSessionId": "otp_sess_9823471092",
  "code": "123456"
}
```
* **Response 200 OK :**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjk5MTIs...",
  "user": {
    "userId": 9912,
    "name": "Alexandre M.",
    "phone": "+243810000000",
    "walletId": "CD0100000099",
    "avatarUrl": "https://cdn.onchat.app/avatars/u9912.jpg",
    "btcAddress": "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
  }
}
```

### 1.3 Vérification des Contacts Actifs (Batch Check)
* **Route :** `POST /api/v1/profile/check-contacts`
* **Description :** Interroge en temps réel le statut d'une liste de numéros de téléphone pour savoir s'ils sont enregistrés sur OnChat.
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "phoneNumbers": [
    "+243810000000",
    "+243820000001",
    "+243990000002"
  ]
}
```
* **Response 200 OK :**
```json
{
  "results": [
    {
      "phone": "+243810000000",
      "onChat": true,
      "userId": 9912,
      "name": "Alexandre M.",
      "walletId": "CD0100000099",
      "avatar": "https://cdn.onchat.app/avatars/u9912.jpg"
    },
    {
      "phone": "+243820000001",
      "onChat": false,
      "userId": null,
      "name": null,
      "walletId": null,
      "avatar": null
    }
  ]
}
```

---

## 2. GESTION DES VISIOCONFÉRENCES & LIVE STAGE

### 2.1 Créer une Salle de Conférence / Live Stage
* **Route :** `POST /api/v1/live/rooms/create`
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "title": "Conférence Business & Écosystème CashPay",
  "description": "Présentation en direct des opportunités d'investissement et démonstration du partage d'écran.",
  "isAudioOnly": false,
  "entryFee": 0.0,
  "scheduledAt": "2026-09-27T15:00:00Z"
}
```
* **Response 201 Created :**
```json
{
  "success": true,
  "roomId": "stage_99812",
  "hostWalletId": "CD0100000099",
  "hostName": "Alexandre M.",
  "title": "Conférence Business & Écosystème CashPay",
  "activeParticipantsCount": 1,
  "isLive": true,
  "createdTime": "2026-09-27T10:30:00Z"
}
```

### 2.2 Obtenir l'État de la Visioconférence (Live State Sync)
* **Route :** `GET /api/v1/live/rooms/{roomId}`
* **Headers :** `Authorization: Bearer <JWT>`
* **Response 200 OK :**
```json
{
  "roomId": "stage_99812",
  "hostName": "Alexandre M.",
  "hostWalletId": "CD0100000099",
  "hostAvatar": "https://cdn.onchat.app/avatars/u9912.jpg",
  "title": "Conférence Business & Écosystème CashPay",
  "isLive": true,
  "isAudioOnly": false,
  "isScreenSharing": true,
  "speakerOnStage": {
    "walletId": "CD0100000014",
    "name": "Aminata S.",
    "avatar": "https://cdn.onchat.app/avatars/u14.jpg",
    "role": "Invité sur scène",
    "isMicMuted": false
  },
  "participantsCount": 24,
  "handRaisedRequests": [
    {
      "walletId": "CD0100000022",
      "name": "Jean-Pierre M.",
      "avatarUrl": "https://cdn.onchat.app/avatars/u22.jpg",
      "requestedAt": "2026-09-27T10:32:10Z"
    }
  ]
}
```

### 2.3 Demander la Parole (Hand Raise Request)
* **Route :** `POST /api/v1/live/rooms/{roomId}/raise-hand`
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "action": "RAISE_HAND"
}
```
* **Response 200 OK :**
```json
{
  "success": true,
  "message": "Demande de prise de parole transmise à l'organisateur"
}
```

### 2.4 Faire Monter un Participant sur Scène (Organisateur Uniquement)
* **Route :** `POST /api/v1/live/rooms/{roomId}/promote-speaker`
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "targetWalletId": "CD0100000014"
}
```
* **Response 200 OK :**
```json
{
  "success": true,
  "activeSpeaker": {
    "walletId": "CD0100000014",
    "name": "Aminata S.",
    "avatar": "https://cdn.onchat.app/avatars/u14.jpg",
    "promotedAt": "2026-09-27T10:33:00Z"
  },
  "previousSpeakerDemoted": true
}
```

### 2.5 Faire Descendre l'Intervenant / Rétrograder
* **Route :** `POST /api/v1/live/rooms/{roomId}/demote-speaker`
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "targetWalletId": "CD0100000014"
}
```
* **Response 200 OK :**
```json
{
  "success": true,
  "message": "L'intervenant a été replacé en mode spectateur"
}
```

### 2.6 Activer ou Couper le Micro d'un Participant
* **Route :** `POST /api/v1/live/rooms/{roomId}/control-microphone`
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "targetWalletId": "CD0100000014",
  "mute": true
}
```
* **Response 200 OK :**
```json
{
  "success": true,
  "targetWalletId": "CD0100000014",
  "isMicMuted": true
}
```

---

## 3. FEED DE VIDÉOS LIVE & REELS (TIKTOK-STYLE)

### 3.1 Obtenir le Flux des Vidéos Live & Événements Programmé
* **Route :** `GET /api/v1/reels/feed`
* **Headers :** `Authorization: Bearer <JWT>`
* **Params :** `category=all&page=1&limit=20`
* **Response 200 OK :**
```json
{
  "reels": [
    {
      "id": "live_reel_1",
      "creatorName": "Alexandre M.",
      "creatorWalletId": "CD0100000099",
      "title": "Conférence Live : Écosystème CashPay & Multi-Transferts",
      "description": "Présentation en direct des nouvelles fonctionnalités de transfert d'argent international.",
      "videoUrl": "https://cdn.onchat.app/streams/live_reel_1.mp4",
      "isLive": true,
      "isScheduled": false,
      "scheduledDate": null,
      "entryFee": 0.0,
      "viewerCount": 1248,
      "likesCount": 540,
      "isLocked": false
    },
    {
      "id": "live_reel_2",
      "creatorName": "Sarah L.",
      "creatorWalletId": "CD0100000088",
      "title": "Webinaire Exclusif : Stratégies Financières Mobile Money",
      "description": "Session VIP de formation en direct.",
      "videoUrl": "https://cdn.onchat.app/streams/live_reel_2.mp4",
      "isLive": false,
      "isScheduled": true,
      "scheduledDate": "Demain à 18:30",
      "entryFee": 5.0,
      "viewerCount": 320,
      "likesCount": 180,
      "isLocked": true
    }
  ]
}
```

### 3.2 Programmer un Événement Live
* **Route :** `POST /api/v1/reels/schedule`
* **Headers :** `Authorization: Bearer <JWT>`
* **Body Request :**
```json
{
  "title": "Formation Trading & Micro-Prêt",
  "description": "Analyse des marchés et questions-réponses en direct.",
  "scheduledTimestamp": 1790610000000,
  "entryFee": 2.50,
  "currency": "USD"
}
```
* **Response 201 Created :**
```json
{
  "success": true,
  "reelId": "scheduled_7712",
  "message": "Événement live programmé avec succès"
}
```

---

## 4. HISTORIQUE DES TRANSACTIONS & FILTRES DE DATES

### 4.1 Récupérer l'Historique des Transactions avec Filtres
* **Route :** `GET /api/v1/wallet/transactions`
* **Headers :** `Authorization: Bearer <JWT>`
* **Query Parameters Optionnels :**
  - `period` = `today` | `yesterday` | `this_week` | `this_month` | `all`
  - `startDate` = `YYYY-MM-DD`
  - `endDate` = `YYYY-MM-DD`
  - `query` = `mot-clef, motif, walletId, date`
* **Response 200 OK :**
```json
{
  "walletId": "CD0100000099",
  "currency": "USD",
  "balance": 1250.00,
  "totalCount": 3,
  "transactions": [
    {
      "id": "tx_889123",
      "type": "TR",
      "direction": "outgoing",
      "amount": 25.00,
      "currency": "USD",
      "senderWalletId": "CD0100000099",
      "senderName": "Alexandre M.",
      "receiverWalletId": "CD0100000014",
      "receiverName": "Aminata S.",
      "description": "Frais d'accès Live VIP",
      "status": "SUCCESS",
      "createdAt": "2026-09-27T10:15:00Z"
    },
    {
      "id": "tx_889122",
      "type": "DP_AGENT",
      "direction": "incoming",
      "amount": 100.00,
      "currency": "USD",
      "senderWalletId": "AGENT_CD_04",
      "senderName": "Agent CashPay Gombe",
      "receiverWalletId": "CD0100000099",
      "receiverName": "Alexandre M.",
      "description": "Dépôt en espèces",
      "status": "SUCCESS",
      "createdAt": "2026-09-26T14:20:00Z"
    }
  ]
}
```

---

## 5. RECOMMANDATIONS COMPORTEMENTALES & CODES DE STATUT

| Code HTTP | Libellé | Signification d'Usage |
|---|---|---|
| `200` | OK | Requête exécutée normalement |
| `201` | Created | Salle ou Transaction créée avec succès |
| `400` | Bad Request | Syntaxe JSON invalide ou paramètres manquants |
| `401` | Unauthorized | Token JWT manquant ou expiré |
| `403` | Forbidden | Privilèges insuffisants (seul l'organisateur peut faire monter sur scène) |
| `404` | Not Found | Salle de visioconférence ou profil introuvable |
| `402` | Payment Required | Solde insuffisant pour accéder au live payant |
