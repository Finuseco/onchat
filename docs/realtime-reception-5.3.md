# OnChat 5.3 — réception des messages

- Endpoint partagé avec Wallet : `wss://app.cashpay-all.com/ws`, configurable par `-PonchatWsUrl=...`.
- Décodage des métadonnées réelles (nombres, objets, booléens), récupération des URL/noms des pièces jointes depuis les métadonnées, conservation de celles-ci dans Room.
- Réception en arrière-plan enregistrée en base locale, insertions transactionnelles et absence de double comptage des messages déjà reçus.
- Rechargement de la conversation ouverte après connexion/reconnexion ; un échec REST n'est plus présenté comme une synchronisation réussie.
- Aucun identifiant de message ou de conversation fictif lors du décodage WebSocket.

Validation : `assembleDebug`, `assembleRelease`, lint vital Release et vérification JVM des adaptateurs Moshi compilés (message vocal avec durée numérique, métadonnées structurées, URL de fichier et événement imbriqué).

Le serveur public `/ws` doit être configuré avec le serveur partagé du backend et ses migrations. Les builds ne prouvent pas le fonctionnement sur le réseau de production. Les appels et notifications nécessitent un essai entre appareils ; le réveil d'une application arrêtée de force nécessite un service push système. Aucun déploiement production effectué.
