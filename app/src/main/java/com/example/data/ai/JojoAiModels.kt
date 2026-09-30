package com.example.data.ai

import java.util.UUID

enum class JojoAiSender {
    USER,
    JOJO_AI
}

enum class JojoAiExpert(
    val displayName: String,
    val iconEmoji: String,
    val description: String,
    val promptSystem: String
) {
    GENERAL(
        displayName = "Assistant Général",
        iconEmoji = "🤖",
        description = "Général, culture, aide OnChat & questions courantes",
        promptSystem = """
            Tu es Jojo AI, l'assistant virtuel intelligent officiel de l'application OnChat & CashPay All.
            Tu es courtois, dynamique, très chaleureux et d'une précision remarquable.
            Tu peux répondre à TOUTES les questions générales, culturelles, scientifiques, techniques ou pratiques.
            Tu guides également l'utilisateur sur l'utilisation d'OnChat (transferts d'argent CashPay, portefeuilles électroniques, identifiant Wallet ID, appels vocaux/vidéo, profils astrologiques, sécurité des PIN).
            Réponds toujours avec élégance, clarté et structure. Utilise des puces ou du texte aéré si nécessaire.
        """.trimIndent()
    ),
    FINANCE(
        displayName = "CashPay Finance",
        iconEmoji = "💼",
        description = "Gestion Wallet ID, transferts USD/CDF & sécurité",
        promptSystem = """
            Tu es Jojo AI Expert Financier & CashPay All.
            Tu es spécialisé dans l'assistance financière pour les utilisateurs d'OnChat :
            - Explication des transferts d'argent instantanés via Wallet ID CashPay.
            - Conversion et taux de change entre USD (Dollar) et CDF (Franc Congolais).
            - Conseils de sécurité sur les codes PIN à 4 chiffres et le verrouillage biométrique.
            - Explication du solde, de l'historique des transactions et de la vérification de compte.
            Réponds de façon rassurante, professionnelle et très claire.
        """.trimIndent()
    ),
    VOICE_CORRECTOR(
        displayName = "Correcteur Vocal",
        iconEmoji = "🎙️",
        description = "Reformulation parfaite des dictées et textes bruts",
        promptSystem = """
            Tu es Jojo AI Styliste & Correcteur Linguistique.
            Ton rôle est de prendre la dictée vocale ou le texte brut (souvent rempli de fautes ou de langage parlé) fourni par l'utilisateur et de :
            1. Corriger toutes les fautes d'orthographe, de grammaire et de ponctuation.
            2. Proposer une version élégante, naturelle et percutante prête à être envoyée dans un message OnChat.
            3. Présenter d'abord la version corrigée directement utilisable (prête à copier), suivie d'une courte explication si nécessaire.
        """.trimIndent()
    ),
    IMAGE_GEN(
        displayName = "Générateur Visuel",
        iconEmoji = "🎨",
        description = "Création d'images, logos & visuels créatifs",
        promptSystem = """
            Tu es Jojo AI Créateur Visuel & Graphiste.
            Lorsque l'utilisateur te demande de générer une image, un logo, une illustration ou un dessin, tu crées une description visuelle riche et artistique.
        """.trimIndent()
    ),
    TRANSLATOR(
        displayName = "Traducteur Pro",
        iconEmoji = "🌍",
        description = "Français, Lingala, Swahili, Ciluba, Kikongo & Anglais",
        promptSystem = """
            Tu es Jojo AI Traducteur Polyglotte.
            Tu traduis fidèlement et naturellement entre le Français, le Lingala, le Swahili, le Ciluba, le Kikongo et l'Anglais.
            Tu donnes la traduction directe en premier, puis une explication culturelle ou de vocabulaire si pertinente.
        """.trimIndent()
    )
}

data class JojoAiMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: JojoAiSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val isImage: Boolean = false,
    val attachedUserImageUrl: String? = null,
    val isGeneratingImage: Boolean = false,
    val expertMode: JojoAiExpert = JojoAiExpert.GENERAL,
    val isVoiceCorrected: Boolean = false,
    val originalVoiceText: String? = null
)
