# Script PowerShell pour démarrer le projet Karhebti Backend

Write-Host "🚀 Démarrage de Karhebti Backend..." -ForegroundColor Green
Write-Host ""

# Vérifier si MongoDB est en cours d'exécution
Write-Host "📦 Vérification de MongoDB..." -ForegroundColor Yellow
$mongoProcess = Get-Process mongod -ErrorAction SilentlyContinue

if (!$mongoProcess) {
    Write-Host "⚠️  MongoDB n'est pas en cours d'exécution." -ForegroundColor Red
    Write-Host "   Veuillez démarrer MongoDB avec la commande: mongod" -ForegroundColor Red
    Write-Host ""
    $response = Read-Host "Voulez-vous continuer quand même? (o/n)"
    if ($response -ne "o") {
        exit
    }
} else {
    Write-Host "✅ MongoDB est en cours d'exécution" -ForegroundColor Green
}

Write-Host ""
Write-Host "🔧 Vérification du fichier .env..." -ForegroundColor Yellow

if (!(Test-Path ".env")) {
    Write-Host "⚠️  Fichier .env non trouvé. Création à partir de .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "✅ Fichier .env créé. Veuillez le configurer avant de continuer." -ForegroundColor Green
    Write-Host ""
    $response = Read-Host "Appuyez sur Entrée pour continuer..."
}

Write-Host ""
Write-Host "📚 L'application va démarrer sur:" -ForegroundColor Cyan
Write-Host "   - API: http://localhost:3000" -ForegroundColor Cyan
Write-Host "   - Swagger: http://localhost:3000/api" -ForegroundColor Cyan
Write-Host ""

# Démarrer l'application
npm run start:dev
