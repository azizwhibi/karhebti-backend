#!/usr/bin/env python3
"""
Script pour tester les notifications WebSocket
Installation: pip install python-socketio requests
Utilisation: python test-websocket.py
"""

import socketio
import requests
import time
import json
from datetime import datetime
import os

# Configuration
BACKEND_URL = os.environ.get('BACKEND_URL', 'http://localhost:3000')
USER_ID = os.environ.get('USER_ID', 'test-user')

# Créer un client Socket.io
sio = socketio.Client(reconnection=True)

class NotificationTester:
    def __init__(self):
        self.connected = False
        self.notifications_received = []
        self.setup_events()

    def setup_events(self):
        """Configurer les événements Socket.io"""
        @sio.event
        def connect():
            self.connected = True
            print("✅ Connecté au serveur WebSocket")
            self.log_message("Connexion établie")

        @sio.on('notification')
        def on_notification(data):
            print(f"📬 Notification reçue: {data}")
            self.notifications_received.append(data)
            self.log_message(f"Notification: {data.get('titre', 'Sans titre')}")

        @sio.event
        def disconnect():
            self.connected = False
            print("❌ Déconnecté du serveur WebSocket")
            self.log_message("Déconnecté")

    def log_message(self, message):
        """Ajouter un message aux logs"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        print(f"[{timestamp}] {message}")

    def connect_to_server(self, token=None):
        """Se connecter au serveur WebSocket"""
        try:
            print(f"🔄 Tentative de connexion à {BACKEND_URL}...")
            auth = {'token': token} if token else None
            sio.connect(BACKEND_URL, auth=auth, wait_timeout=10)
            return True
        except Exception as e:
            print(f"❌ Erreur de connexion: {e}")
            return False

    def disconnect(self):
        """Se déconnecter du serveur"""
        if sio.connected:
            sio.disconnect()
            self.connected = False
            print("✅ Déconnecté")

    def send_notification_http(self, titre, message, type_notif):
        """Envoyer une notification via HTTP"""
        try:
            print(f"\n📤 Envoi via HTTP...")
            url = f"{BACKEND_URL}/api/notifications/send"
            payload = {
                'userId': USER_ID,
                'titre': titre,
                'message': message,
                'type': type_notif
            }
            response = requests.post(url, json=payload, timeout=5)
            if response.status_code == 200:
                print(f"✅ Envoyée avec succès (Status: {response.status_code})")
            else:
                print(f"⚠️  Status: {response.status_code}")
        except Exception as e:
            print(f"❌ Erreur: {e}")

    def send_notification_socketio(self, titre, message, type_notif):
        """Envoyer une notification via Socket.io"""
        if not self.connected:
            print("❌ Non connecté au serveur")
            return

        try:
            print(f"\n📤 Envoi via Socket.io...")
            data = {
                'titre': titre,
                'message': message,
                'type': type_notif,
                'timestamp': datetime.now().isoformat()
            }
            sio.emit('notification', data)
            print("✅ Notification émise")
        except Exception as e:
            print(f"❌ Erreur: {e}")

    def verify_connection(self):
        """Vérifier la connexion au serveur"""
        try:
            response = requests.get(f"{BACKEND_URL}/health", timeout=5)
            if response.status_code == 200:
                print(f"✅ Serveur actif (Status: {response.status_code})")
                return True
            else:
                print(f"⚠️  Serveur répond: {response.status_code}")
                return False
        except:
            print("❌ Serveur inactif")
            return False

    def show_menu(self):
        """Afficher le menu principal"""
        os.system('clear' if os.name != 'nt' else 'cls')
        print("╔════════════════════════════════════════════════════════════╗")
        print("║        WebSocket Notification Tester (Python)              ║")
        print("╚════════════════════════════════════════════════════════════╝")
        print(f"\n🔗 Backend URL: {BACKEND_URL}")
        print(f"👤 User ID: {USER_ID}")
        print(f"📡 Connecté: {'✅ Oui' if self.connected else '❌ Non'}")
        print(f"📬 Notifications reçues: {len(self.notifications_received)}")
        print("\n" + "="*60)
        print("\n1. Se connecter au serveur")
        print("2. Se déconnecter")
        print("3. Envoyer notification 'Bienvenue'")
        print("4. Envoyer notification 'Maintenance'")
        print("5. Envoyer notification 'Erreur'")
        print("6. Envoyer notification personnalisée")
        print("7. Vérifier la connexion au serveur")
        print("8. Afficher notifications reçues")
        print("9. Effacer les notifications")
        print("0. Quitter")
        print("\n" + "="*60)

    def show_received_notifications(self):
        """Afficher les notifications reçues"""
        if not self.notifications_received:
            print("\n❌ Aucune notification reçue")
        else:
            print("\n📬 Notifications reçues:")
            for i, notif in enumerate(self.notifications_received, 1):
                print(f"\n  {i}. Titre: {notif.get('titre', 'N/A')}")
                print(f"     Message: {notif.get('message', 'N/A')}")
                print(f"     Type: {notif.get('type', 'N/A')}")

    def run(self):
        """Boucle principale"""
        while True:
            self.show_menu()
            choice = input("Sélectionner une option: ").strip()

            if choice == "1":
                self.connect_to_server()
            elif choice == "2":
                self.disconnect()
            elif choice == "3":
                self.send_notification_socketio("Bienvenue", "Vous êtes connecté!", "welcome")
            elif choice == "4":
                self.send_notification_socketio("Maintenance", "Maintenance prévue à 22h00", "maintenance")
            elif choice == "5":
                self.send_notification_socketio("Erreur", "Une erreur s'est produite", "error")
            elif choice == "6":
                titre = input("Titre: ").strip()
                message = input("Message: ").strip()
                type_notif = input("Type: ").strip()
                self.send_notification_socketio(titre, message, type_notif)
            elif choice == "7":
                self.verify_connection()
            elif choice == "8":
                self.show_received_notifications()
            elif choice == "9":
                self.notifications_received = []
                print("✅ Notifications effacées")
            elif choice == "0":
                print("Au revoir!")
                self.disconnect()
                break
            else:
                print("❌ Option invalide")

            input("\nAppuyer sur Entrée pour continuer...")

if __name__ == "__main__":
    print("\n⚙️  Installation des dépendances si nécessaire...")
    print("pip install python-socketio requests\n")

    tester = NotificationTester()
    try:
        tester.run()
    except KeyboardInterrupt:
        print("\n\nInterruption utilisateur")
        tester.disconnect()
    except Exception as e:
        print(f"\n❌ Erreur: {e}")
        tester.disconnect()

