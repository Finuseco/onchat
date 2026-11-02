# Notifications et lecture OnChat 5.1

- Le service WebSocket était absent du manifeste et n'était pas démarré à la connexion. Il est déclaré avec les permissions nécessaires, lancé après authentification et arrêté à la déconnexion.
- Le service restaure la session existante depuis Room ; les messages sortants n'affichent pas une notification de réception. Les notifications système sont réparties entre premier plan et arrière-plan pour éviter les doublons.
- Le statut de connexion du service est réel (connecté/reconnexion), sans promesse « 24/7 ». Le clic sur une notification ouvre la conversation après authentification.
- Les vidéos envoyées dans un chat utilisent le dépôt privé des pièces jointes, sans publication publique en reel. La conversation destinataire est capturée avant l’upload. Les erreurs de fichier ou d’audio sont remontées.
- Les pièces jointes utilisent le type MIME du fournisseur Android au lieu de forcer JPEG, MP4 ou PDF sur tous les fichiers.
- Le lecteur affiche la miniature retournée par l'API avant la première image, puis un grand indicateur de chargement autour du bouton lecture. Il conserve la lecture automatique après mise en mémoire tampon et permet une relance après erreur.
- Les commentaires restent dans le champ si l'envoi échoue, une erreur est visible et les envois multiples sont empêchés. La lecture se suspend pendant les panneaux commentaires/son/profil et reprend à leur fermeture.
- La page de réunion affiche la progression réelle du chargement. Les anciennes valeurs d'identité utilisateur 101 ne sont plus utilisées pour la gestion des réunions.
- Version Android : 5.1 / code 6.

## Vérifications et conditions

La compilation et son résultat sont consignés dans le rapport de livraison. Aucun compte réel ni téléphone connecté n'est disponible dans cet environnement : réception sur écran verrouillé, comportement audio, caméra et export matériel demandent une validation sur appareils.

Android peut limiter ou arrêter les services, en particulier les services `dataSync` sur Android 15. Cette livraison ne remplace pas Firebase Cloud Messaging et ne garantit pas une réception lorsque le processus est arrêté ou le service limité par le système. Une intégration push serveur/app avec configuration Firebase reste nécessaire pour ce cas.

Le backend doit recevoir la migration de compatibilité des réunions fournie dans cashpay_web, et ffmpeg pour générer les miniatures des nouvelles publications. Les anciennes vidéos sans miniature ne sont pas réencodées automatiquement. La réutilisation du son reste subordonnée à l'autorisation explicite de l'auteur et à la migration `onchat_video_sounds` existante ; les droits ne sont pas accordés rétroactivement.
