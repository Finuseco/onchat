# OnChat Android 5.2 — appels directs

Les appels audio et vidéo ouvrent un écran plein écran : avatar et attente pour l'audio, vidéo distante et aperçu local pour la caméra, commandes microphone, haut-parleur, caméra et raccrochage. L'acceptation conserve l'identifiant serveur. Les invitations et l'ouverture explicite d'un lien permettent d'ajouter des participants à la même salle.

Le média est transporté par WebRTC dans le WebView authentifié ; les événements passent par le serveur WebSocket public configuré. L'indication de connexion suit l'état RTC. Le service au premier plan adapte ses types microphone/caméra aux permissions et à l'état réel. La sortie haut-parleur utilise le pont Android. L'historique est rechargé au retour sur l'écran appels.

Prérequis : déployer le backend correspondant et la migration des appels directs, configurer WSS et TURN, mettre à jour le WebView. Ne pas utiliser l'adresse localhost du serveur comme adresse publique. Les deux téléphones doivent utiliser la version compatible.

Validation : assembleDebug, assembleRelease et lintVitalRelease ont réussi avec JDK 17 / SDK 35. Aucun test sur deux téléphones physiques, réseau opérateur, réception en arrière-plan ou TURN de production n'a été effectué. Le paquet release est non signé : une signature officielle reste nécessaire. Aucun APK, secret, fixture ou test temporaire n'est ajouté au dépôt.
