# OnChat 5.4 (9) — réception et notifications

Messages, images, audios, vidéos et partages conservent leurs métadonnées structurées jusqu'au rendu. Les événements rejoués sont dédupliqués par identifiant serveur dans une fenêtre bornée, réinitialisée à la déconnexion du compte. Les groupes reçus sont marqués comme groupes et leur nom est rétabli à l'ouverture.

La synchronisation REST met à jour les conversations dans une transaction sans les vider préalablement ; elle garde les informations locales et les événements plus récents et refuse une réponse d'une ancienne session. Le chat ouvert et les groupes sont resynchronisés après connexion/reconnexion. Les erreurs ne sont plus retournées comme un succès de synchronisation.

Les alertes photo/audio portent un libellé même sans texte. Les appels ont une durée de sonnerie bornée, un seul propriétaire de notification selon la visibilité de l'app, et une annulation dans le service d'arrière-plan sur acceptation/refus/fin. Les identifiants d'appel ne sont plus inventés et les alias historiques de fin d'appel sont reconnus.

Backend associé : Finuseco/cashpay_web PR 3, document `docs/onchat-realtime-recovery-5.4.md`. Le `/ws` public doit être mis à jour avec ce serveur ; le simple changement du nom de domaine ne suffit pas.

Les tests temporaires restent hors Git. Les flux caméra/micro entre deux appareils et le réveil après arrêt forcé ne sont pas déclarés testés ; ce dernier cas nécessite un push système.

Validation finale : `assembleDebug`, `assembleRelease`, `lintVitalRelease` réussis ; tests JVM sur les vraies classes du build pour la déduplication, les événements d'appel, les médias de groupe et les métadonnées de partage.
