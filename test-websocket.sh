#!/bin/bash
# Script pour tester les notifications WebSocket
# Utilisation: chmod +x test-websocket.sh && ./test-websocket.sh

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:3000}"
USER_ID="${USER_ID:-test-user}"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║        WebSocket Notification Tester                        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Backend URL: $BACKEND_URL"
echo "User ID: $USER_ID"
echo ""

# Fonction pour envoyer une notification
send_notification() {
    local titre=$1
    local message=$2
    local type=$3

    echo "📤 Envoi de notification..."
    echo "  Titre: $titre"
    echo "  Message: $message"
    echo "  Type: $type"
    echo ""

    curl -X POST "$BACKEND_URL/api/notifications/send" \
        -H "Content-Type: application/json" \
        -d "{
            \"userId\": \"$USER_ID\",
            \"titre\": \"$titre\",
            \"message\": \"$message\",
            \"type\": \"$type\"
        }" \
        -s -o /dev/null -w "Status: %{http_code}\n\n"
}

# Fonction pour envoyer via Socket.io
send_socketio() {
    echo "⚡ Test Socket.io direct"
    echo "Utilisez un client Socket.io pour envoyer:"
    echo ""
    echo "socket.emit('notification', {"
    echo "  titre: '$1',"
    echo "  message: '$2',"
    echo "  type: '$3'"
    echo "});"
    echo ""
}

# Menu
while true; do
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                    Menu Principal                           ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "1. Envoyer notification 'Bienvenue'"
    echo "2. Envoyer notification 'Maintenance'"
    echo "3. Envoyer notification 'Erreur'"
    echo "4. Envoyer notification personnalisée"
    echo "5. Afficher commande Socket.io"
    echo "6. Vérifier la connexion au serveur"
    echo "7. Quitter"
    echo ""
    read -p "Sélectionner une option: " choice

    case $choice in
        1)
            send_notification "Bienvenue" "Vous êtes connecté!" "welcome"
            ;;
        2)
            send_notification "Maintenance" "Maintenance prévue à 22h00" "maintenance"
            ;;
        3)
            send_notification "Erreur" "Une erreur s'est produite" "error"
            ;;
        4)
            read -p "Titre: " titre
            read -p "Message: " message
            read -p "Type: " type
            send_notification "$titre" "$message" "$type"
            ;;
        5)
            send_socketio "Test" "Ceci est un test" "test"
            ;;
        6)
            echo "🔍 Vérification du serveur..."
            curl -s -o /dev/null -w "Status: %{http_code}\n" "$BACKEND_URL/health" && echo "✅ Serveur actif" || echo "❌ Serveur inactif"
            echo ""
            ;;
        7)
            echo "Au revoir!"
            exit 0
            ;;
        *)
            echo "❌ Option invalide"
            ;;
    esac

    echo ""
    read -p "Appuyer sur Entrée pour continuer..."
    clear
done

